package cl.kylapse.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.net.http.SslError;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceResponse;
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.LinearLayout;
import android.content.SharedPreferences;
import android.widget.TextView;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kylapse: abre Synapse (HSJD) dentro de la app y carga el programa Kylapse
 * (https://kylapse.github.io/kylapse/sm.js) en la lista de trabajo y en el visor.
 * La app no guarda ni envía datos: el inicio de sesión y las imágenes van directo a Synapse.
 */
public class MainActivity extends Activity {
    private static final String[][] HOSPITALS = {
        {"Hospital San Juan de Dios", "sjdpacs.synapsetimed.cl"},
        {"Servicio de Salud Metropolitano Norte", "smnpacs5.synapsetimed.cl"},
    };
    private volatile String host = "";

    private String start() { return "https://" + host + "/WorkflowUI/"; }
    private static boolean isSynapse(String h) { return h != null && h.endsWith(".synapsetimed.cl"); }
    /** La pantalla de inicio de sesión de Synapse se ve muy chica en el teléfono: se acerca a su tarjeta central. */
    private static final String ZOOM_LOGIN =
        "(function(){var m=document.querySelector('meta[name=viewport]');if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m)}" +
        "m.content='width=560';})();";
    private static final String LOADER =
        "(function(){window.__KYL_APP=1;if(window.__SM||window.__SMV||window.__KYL_LOADING)return;" +
        "window.__KYL_LOADING=1;var s=document.createElement('script');" +
        "s.src='https://kylapse.github.io/kylapse/sm.js?v='+Date.now();" +
        "s.onerror=function(){window.__KYL_LOADING=0};" +
        "(document.body||document.documentElement).appendChild(s)})();";

    private WebView web;
    private SharedPreferences prefs;
    private LinearLayout chooser;
    private TextView status;
    private boolean failed = false;
    private final ArrayDeque<String> console = new ArrayDeque<>();
    private final Runnable check = this::checkBlank;
    private String authInfo = "";
    private String mobileUA, desktopUA;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable slow = () -> {
        if (status.getVisibility() == View.VISIBLE && !failed)
            status.setText("Synapse está tardando en responder…\n\nSi no carga, revisa que tengas conexión a la red desde donde normalmente abres Synapse (WiFi del hospital o VPN).");
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.parseColor("#0D1A19"));
        getWindow().setNavigationBarColor(Color.parseColor("#0D1A19"));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#122221"));
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#122221"));
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        status = new TextView(this);
        status.setTextColor(Color.parseColor("#E6EEEC"));
        status.setTextSize(17);
        status.setGravity(Gravity.CENTER);
        status.setPadding(60, 60, 60, 60);
        status.setBackgroundColor(Color.parseColor("#122221"));
        status.setText("KYLAPSE\n\nAbriendo Synapse…");
        status.setOnClickListener(v -> { if (failed) retry(); });
        root.addView(status, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        // Para iniciar sesión la app se presenta como Chrome de teléfono (igual que el Chrome que sí funciona);
        // ya dentro de Synapse cambia a modo escritorio para el visor.
        String defUA = s.getUserAgentString();
        mobileUA = chromeMobileUserAgent(defUA);
        desktopUA = desktopUserAgent(defUA);
        s.setUserAgentString(mobileUA);
        currentUA = mobileUA;
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST))
                WebSettingsCompat.setRequestedWithHeaderOriginAllowList(s, Collections.emptySet());
        } catch (Throwable ignored) { }

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(web, true);

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage m) {
                if (m.messageLevel() == ConsoleMessage.MessageLevel.ERROR || m.messageLevel() == ConsoleMessage.MessageLevel.WARNING) {
                    String src = m.sourceId() == null ? "" : m.sourceId().replaceAll("^https?://[^/]+", "").replaceAll("\\?.*$", "");
                    String line = m.message() + "  [" + src + ":" + m.lineNumber() + "]";
                    if (line.length() > 160) line = line.substring(0, 160) + "…";
                    console.addLast(line);
                    while (console.size() > 6) console.removeFirst();
                }
                return false;
            }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if ("kylapse".equals(u.getScheme())) { showChooser(); return true; }
                noteAuth(u);
                String h = u.getHost();
                if (isSynapse(h)) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                return fetchWithoutAppHeader(req);
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                if (url != null) noteAuth(Uri.parse(url));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!failed) status.setVisibility(View.GONE);
                switchToDesktopIfInside(url);
                inject(url);
                handler.removeCallbacks(check);
                handler.postDelayed(check, 5000);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame())
                    fail("No se pudo conectar con Synapse.\n\n" + err.getDescription() + " (" + err.getErrorCode() + ")\n\n"
                        + "Synapse solo abre desde ciertas redes. Prueba conectado al WiFi del hospital o con la VPN, o revisa que abra en Chrome.");
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest req, WebResourceResponse resp) {
                if (req.isForMainFrame() && resp.getStatusCode() >= 500)
                    fail("Synapse respondió con un error (" + resp.getStatusCode() + ").");
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler h, SslError err) {
                h.cancel();
                fail("No se pudo verificar la conexión segura con Synapse (código " + err.getPrimaryError() + ").\n\n"
                    + "Por seguridad la app no continúa. Avísale a quien te compartió Kylapse.");
            }
        });

        prefs = getSharedPreferences("kylapse", MODE_PRIVATE);
        host = prefs.getString("host", "");
        buildChooser(root);
        if (state != null && !host.isEmpty()) web.restoreState(state);
        else if (host.isEmpty()) showChooser();
        else web.loadUrl(start());
        handler.postDelayed(slow, 15000);
    }

    /** Si la página quedó vacía (pantalla negra), muestra en pantalla qué pasó para poder diagnosticarlo. */
    private void checkBlank() {
        if (failed) return;
        web.evaluateJavascript("(function(){var b=document.body;var f=document.forms[0],fa='';try{fa=f?new URL(f.action,location.href).pathname:''}catch(e){};var hid=Array.prototype.map.call(document.querySelectorAll('input'),function(i){return i.name+(/^(error|error_description|error_uri)$/.test(i.name)?'='+i.value:'')}).join(',');return [location.pathname,document.title,b?b.innerText.trim().length:-1,b?b.innerHTML.length:-1,document.querySelectorAll('iframe').length,innerWidth+'x'+innerHeight,(/^\\s*Error/m.test(b?b.innerText:'')&&!document.querySelector('input[type=password]'))?'ERRPAGE:'+(b.innerText.replace(/\\s+/g,' ').slice(0,160)):'','form:'+fa,'campos:'+hid].join(' | ')})()", r -> {
            if (r == null) return;
            String v = r.replace("\"", "");
            String[] p = v.split(" \\| ");
            int n = -1;
            try { n = Integer.parseInt(p[2].trim()); } catch (Exception ignored) { }
            boolean viewer = v.toLowerCase().contains("/imageviewer/");
            boolean ssoError = v.toLowerCase().contains("/synapsesignon/") && v.contains("ERRPAGE");
            if ((n >= 0 && n < 5 && !viewer) || ssoError) {
                StringBuilder sb = new StringBuilder("Synapse no dejó entrar.\n\nDatos para Alberto:\n").append(v).append("\n");
                for (String c : console) sb.append("\n• ").append(c);
                if (console.isEmpty()) sb.append("\n(sin errores en consola)");
                if (!authInfo.isEmpty()) sb.append("\n\nSolicitud:\n").append(authInfo);
                sb.append("\n\nIdioma: ").append(java.util.Locale.getDefault().toLanguageTag()).append("\nNavegador: ").append(web.getSettings().getUserAgentString());
                fail(sb.toString());
            }
        });
    }

    private volatile String currentUA = "";

    /**
     * Android agrega a cada página la cabecera "X-Requested-With: cl.kylapse.app". Al verla, Synapse arma
     * mal el inicio de sesión (sin nonce) y falla. Aquí la app pide ella misma las páginas principales de
     * Synapse, con las mismas cookies y cabeceras pero sin esa marca. Si algo falla, deja que WebView siga normal.
     */
    private WebResourceResponse fetchWithoutAppHeader(WebResourceRequest req) {
        try {
            if (!req.isForMainFrame() || !"GET".equalsIgnoreCase(req.getMethod())) return null;
            Uri u = req.getUrl();
            if (!isSynapse(u.getHost())) return null;
            String path = u.getPath() == null ? "" : u.getPath().toLowerCase();
            if (!(path.startsWith("/workflowui") || path.startsWith("/imageviewer"))) return null;

            String url = u.toString();
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(20000);
            c.setReadTimeout(30000);
            for (Map.Entry<String, String> h : req.getRequestHeaders().entrySet()) {
                String k = h.getKey();
                if (k == null || k.equalsIgnoreCase("X-Requested-With") || k.equalsIgnoreCase("Accept-Encoding")
                        || k.equalsIgnoreCase("Cookie") || k.equalsIgnoreCase("User-Agent")) continue;
                c.setRequestProperty(k, h.getValue());
            }
            c.setRequestProperty("User-Agent", currentUA);
            CookieManager cm = CookieManager.getInstance();
            String ck = cm.getCookie(url);
            if (ck != null && !ck.isEmpty()) c.setRequestProperty("Cookie", ck);

            int code = c.getResponseCode();
            Map<String, List<String>> hf = c.getHeaderFields();
            for (Map.Entry<String, List<String>> e : hf.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase("Set-Cookie"))
                    for (String v : e.getValue()) cm.setCookie(url, v);
            }
            cm.flush();

            Map<String, String> noCache = new HashMap<>();
            noCache.put("Cache-Control", "no-store");
            if (code >= 300 && code < 400) {
                String loc = c.getHeaderField("Location");
                c.disconnect();
                if (loc == null) return null;
                String abs = new URL(new URL(url), loc).toString();
                noteAuth(Uri.parse(abs));
                String html = "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width\"></head>"
                    + "<body style=\"background:#122221\"><script>location.replace(" + jsString(abs) + ");</script></body></html>";
                return new WebResourceResponse("text/html", "utf-8", 200, "OK", noCache,
                    new ByteArrayInputStream(html.getBytes("UTF-8")));
            }

            String ct = c.getContentType();
            String mime = "text/html", charset = "utf-8";
            if (ct != null) {
                String[] parts = ct.split(";");
                mime = parts[0].trim();
                for (String p : parts) {
                    p = p.trim();
                    if (p.toLowerCase().startsWith("charset=")) charset = p.substring(8).replace("\"", "").trim();
                }
            }
            Map<String, String> rh = new HashMap<>();
            for (Map.Entry<String, List<String>> e : hf.entrySet()) {
                String k = e.getKey();
                if (k == null || e.getValue().isEmpty()) continue;
                String kl = k.toLowerCase();
                if (kl.equals("set-cookie") || kl.equals("content-length") || kl.equals("content-encoding")
                        || kl.equals("transfer-encoding") || kl.equals("connection")) continue;
                rh.put(k, e.getValue().get(0));
            }
            InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
            if (is == null) is = new ByteArrayInputStream(new byte[0]);
            String reason = c.getResponseMessage();
            if (reason == null || reason.trim().isEmpty()) reason = "OK";
            return new WebResourceResponse(mime, charset, code, reason, rh, is);
        } catch (Exception e) {
            return null;
        }
    }

    private static String jsString(String v) {
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : v.toCharArray()) {
            if (ch == '"' || ch == '\\') sb.append('\\').append(ch);
            else if (ch < 0x20 || ch == '<' || ch == '>') sb.append(String.format("\\u%04x", (int) ch));
            else sb.append(ch);
        }
        return sb.append('"').toString();
    }

    /** Guarda los parámetros de la solicitud de inicio de sesión (sin claves ni tokens) para el diagnóstico. */
    private void noteAuth(Uri u) {
        String p = u.getPath();
        if (p == null || !p.toLowerCase().contains("/connect/authorize")) return;
        StringBuilder sb = new StringBuilder();
        for (String k : u.getQueryParameterNames()) {
            String v = u.getQueryParameter(k);
            if (v == null) v = "";
            boolean show = k.matches("client_id|response_type|response_mode|scope|redirect_uri|prompt|ui_locales|acr_values|max_age|display");
            sb.append(k).append(show ? "=" + v : "(" + v.length() + ")").append("  ");
        }
        authInfo = (authInfo.isEmpty() ? "" : authInfo + "\n→ ") + sb.toString().trim() + "  [largo URL " + u.toString().length() + "]";
        if (authInfo.length() > 3000) authInfo = authInfo.substring(authInfo.length() - 3000);
    }

    /**
     * Sin sesión iniciada, Synapse puede pedir un ingreso "silencioso" (prompt=none) que dentro de la app
     * termina en error en vez de mostrar la pantalla de usuario y contraseña. Aquí se quita esa opción para
     * que Synapse muestre siempre su pantalla de inicio de sesión. Devuelve null si no hay nada que cambiar.
     */
    private Uri fixAuth(Uri u) {
        String p = u.getPath();
        if (p == null || !p.toLowerCase().contains("/connect/authorize")) return null;
        String rt = u.getQueryParameter("response_type");
        if (rt == null || !rt.contains("id_token") || u.getQueryParameter("nonce") != null) return null;
        // Se agrega el nonce al final del texto original, sin reconstruir el resto de la solicitud.
        String raw = u.toString();
        String nonce = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        authInfo += "\n(app: agregó nonce)";
        return Uri.parse(raw + (raw.contains("?") ? "&" : "?") + "nonce=" + nonce);
    }

    /** Pantalla para elegir el hospital (se recuerda; se puede cambiar desde el botón Hospital de Kylapse). */
    private void buildChooser(FrameLayout root) {
        chooser = new LinearLayout(this);
        chooser.setOrientation(LinearLayout.VERTICAL);
        chooser.setGravity(Gravity.CENTER);
        chooser.setPadding(70, 70, 70, 70);
        chooser.setBackgroundColor(Color.parseColor("#122221"));
        chooser.setClickable(true);
        TextView t = new TextView(this);
        t.setText("KYLAPSE\n\nElige tu Synapse");
        t.setTextColor(Color.parseColor("#E6EEEC"));
        t.setTextSize(20);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, 0, 0, 50);
        chooser.addView(t);
        for (String[] h : HOSPITALS) {
            Button b = new Button(this);
            b.setText(h[0]);
            b.setAllCaps(false);
            b.setTextSize(17);
            b.setTextColor(Color.WHITE);
            b.setBackgroundColor(Color.parseColor("#38946D"));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, 18, 0, 18);
            b.setPadding(30, 40, 30, 40);
            final String target = h[1];
            b.setOnClickListener(v -> chooseHospital(target));
            chooser.addView(b, lp);
        }
        chooser.setVisibility(View.GONE);
        root.addView(chooser, new FrameLayout.LayoutParams(-1, -1));
    }

    private void showChooser() {
        chooser.setVisibility(View.VISIBLE);
        status.setVisibility(View.GONE);
    }

    private void chooseHospital(String h) {
        host = h;
        prefs.edit().putString("host", h).apply();
        chooser.setVisibility(View.GONE);
        status.setVisibility(View.VISIBLE);
        retry();
    }

    private void fail(String msg) {
        failed = true;
        status.setTextSize(13);
        status.setGravity(Gravity.TOP | Gravity.START);
        status.setMovementMethod(new android.text.method.ScrollingMovementMethod());
        status.setText(msg + "\n\nToca la pantalla para reintentar.");
        status.setVisibility(View.VISIBLE);
    }

    private void retry() {
        failed = false;
        status.setTextSize(17);
        status.setGravity(Gravity.CENTER);
        status.setText("KYLAPSE\n\nAbriendo Synapse…");
        web.loadUrl(start());
        handler.removeCallbacks(slow);
        handler.postDelayed(slow, 15000);
    }

    private void inject(String url) {
        if (url == null) return;
        Uri u = Uri.parse(url);
        String path = u.getPath() == null ? "" : u.getPath().toLowerCase();
        if (!isSynapse(u.getHost())) return;
        String p0 = u.getPath() == null ? "" : u.getPath().toLowerCase();
        if (p0.startsWith("/synapsesignon")) web.evaluateJavascript(ZOOM_LOGIN, null);
        boolean worklist = path.startsWith("/workflowui") && !path.contains("powerjacket");
        boolean viewer = path.startsWith("/imageviewer/");
        if (worklist || viewer) web.evaluateJavascript(LOADER, null);
    }

    private void switchToDesktopIfInside(String url) {
        if (url == null) return;
        Uri u = Uri.parse(url);
        String path = u.getPath() == null ? "" : u.getPath().toLowerCase();
        if (isSynapse(u.getHost()) && path.startsWith("/workflowui")) {
            WebSettings s = web.getSettings();
            if (!desktopUA.equals(s.getUserAgentString())) { s.setUserAgentString(desktopUA); currentUA = desktopUA; }
        }
    }

    /** Como Chrome en un teléfono Android, sin las marcas de "app con navegador integrado". */
    private static String chromeMobileUserAgent(String ua) {
        Matcher m = Pattern.compile("Chrome/([0-9]+)").matcher(ua == null ? "" : ua);
        String major = m.find() ? m.group(1) : "120";
        return "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + major + ".0.0.0 Mobile Safari/537.36";
    }

    /** Igual que "Sitio de escritorio" de Chrome: Synapse se ve y funciona como en el computador. */
    private static String desktopUserAgent(String ua) {
        Matcher m = Pattern.compile("Chrome/([0-9.]+)").matcher(ua == null ? "" : ua);
        String ver = m.find() ? m.group(1) : "120.0.0.0";
        return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + ver + " Safari/537.36";
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (chooser.getVisibility() == View.VISIBLE && !host.isEmpty()) chooser.setVisibility(View.GONE);
        else if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
