package cl.kylapse.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.parseColor("#0D1A19"));
        getWindow().setNavigationBarColor(Color.parseColor("#0D1A19"));

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#122221"));
        setContentView(web);

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

        web.setWebChromeClient(new WebChromeClient());
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
                inject(url);
            }
        });

        if (state != null) web.restoreState(state);
        else web.loadUrl(START);
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
