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
import android.widget.TextView;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kylapse: abre Synapse (HSJD) dentro de la app y carga el programa Kylapse
 * (https://kylapse.github.io/kylapse/sm.js) en la lista de trabajo y en el visor.
 * La app no guarda ni envía datos: el inicio de sesión y las imágenes van directo a Synapse.
 */
public class MainActivity extends Activity {
    private static final String HOST = "sjdpacs.synapsetimed.cl";
    private static final String START = "https://" + HOST + "/WorkflowUI/";
    private static final String LOADER =
        "(function(){window.__KYL_APP=1;if(window.__SM||window.__SMV||window.__KYL_LOADING)return;" +
        "window.__KYL_LOADING=1;var s=document.createElement('script');" +
        "s.src='https://kylapse.github.io/kylapse/sm.js?v='+Date.now();" +
        "s.onerror=function(){window.__KYL_LOADING=0};" +
        "(document.body||document.documentElement).appendChild(s)})();";

    private WebView web;
    private TextView status;
    private boolean failed = false;
    private final ArrayDeque<String> console = new ArrayDeque<>();
    private final Runnable check = this::checkBlank;
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
        s.setUserAgentString(desktopUserAgent(s.getUserAgentString()));

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
                String h = u.getHost();
                if (h != null && (h.equals(HOST) || h.endsWith(".synapsetimed.cl"))) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!failed) status.setVisibility(View.GONE);
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

        if (state != null) web.restoreState(state);
        else web.loadUrl(START);
        handler.postDelayed(slow, 15000);
    }

    /** Si la página quedó vacía (pantalla negra), muestra en pantalla qué pasó para poder diagnosticarlo. */
    private void checkBlank() {
        if (failed) return;
        web.evaluateJavascript("(function(){var b=document.body;return [location.pathname,document.title,b?b.innerText.trim().length:-1,b?b.innerHTML.length:-1,document.querySelectorAll('iframe').length,innerWidth+'x'+innerHeight].join(' | ')})()", r -> {
            if (r == null) return;
            String v = r.replace("\"", "");
            String[] p = v.split(" \\| ");
            int n = -1;
            try { n = Integer.parseInt(p[2].trim()); } catch (Exception ignored) { }
            boolean viewer = v.toLowerCase().contains("/imageviewer/");
            if (n >= 0 && n < 5 && !viewer) {
                StringBuilder sb = new StringBuilder("La página de Synapse quedó en blanco.\n\nDatos para Alberto:\n").append(v).append("\n");
                for (String c : console) sb.append("\n• ").append(c);
                if (console.isEmpty()) sb.append("\n(sin errores en consola)");
                fail(sb.toString());
            }
        });
    }

    private void fail(String msg) {
        failed = true;
        status.setText(msg + "\n\nToca la pantalla para reintentar.");
        status.setVisibility(View.VISIBLE);
    }

    private void retry() {
        failed = false;
        status.setText("KYLAPSE\n\nAbriendo Synapse…");
        web.loadUrl(START);
        handler.removeCallbacks(slow);
        handler.postDelayed(slow, 15000);
    }

    private void inject(String url) {
        if (url == null) return;
        Uri u = Uri.parse(url);
        String path = u.getPath() == null ? "" : u.getPath().toLowerCase();
        if (!HOST.equals(u.getHost())) return;
        boolean worklist = path.startsWith("/workflowui") && !path.contains("powerjacket");
        boolean viewer = path.startsWith("/imageviewer/");
        if (worklist || viewer) web.evaluateJavascript(LOADER, null);
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
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
