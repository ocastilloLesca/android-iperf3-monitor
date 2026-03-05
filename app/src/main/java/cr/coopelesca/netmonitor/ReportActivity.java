package cr.coopelesca.netmonitor;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import java.io.*;

public class ReportActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        Button btnVolver = findViewById(R.id.btnVolver);
        WebView webView  = findViewById(R.id.webView);

        btnVolver.setOnClickListener(v -> finish());

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);

        String htmlPath = getIntent().getStringExtra("htmlPath");
        if (htmlPath != null && !htmlPath.isEmpty()) {
            try {
                FileInputStream fis = new FileInputStream(htmlPath);
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append("\n");
                br.close();
                webView.loadDataWithBaseURL("file:///", sb.toString(), "text/html", "UTF-8", null);
            } catch (Exception e) {
                webView.loadData("<h2>Error cargando reporte: " + e.getMessage() + "</h2>",
                        "text/html", "UTF-8");
            }
        }
    }
}
