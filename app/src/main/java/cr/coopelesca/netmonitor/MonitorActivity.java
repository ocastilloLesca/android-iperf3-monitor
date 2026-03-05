package cr.coopelesca.netmonitor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MonitorActivity extends Activity {

    private TextView tvLog, tvEstado, tvIter, tvTiempoRestante;
    private ScrollView scrollLog;
    private Button btnDetener, btnReporte;

    private String server, port, pingHost, proto, udpBw;
    private int dias, horas, minutos, intMin, intSec, testDur;
    private int intervalo;
    private long endTs, startTs;

    private boolean running = false;
    private int iteracion = 0;
    private Thread monitorThread;
    private Handler uiHandler;

    private StringBuilder logBuffer = new StringBuilder();
    private List<String> dataPoints = new ArrayList<>();
    private String htmlPath = "";
    private String startTime = "";

    private String wifiSSID = "N/A";
    private String wifiBSSID = "N/A";
    private int wifiRSSI = 0;
    private int wifiFreq = 0;
    private String wifiStandard = "N/A";
    private String wifiLinkSpeed = "N/A";

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);

        tvLog            = findViewById(R.id.tvLog);
        tvEstado         = findViewById(R.id.tvEstado);
        tvIter           = findViewById(R.id.tvIter);
        tvTiempoRestante = findViewById(R.id.tvTiempoRestante);
        scrollLog        = findViewById(R.id.scrollLog);
        btnDetener       = findViewById(R.id.btnDetener);
        btnReporte       = findViewById(R.id.btnReporte);

        uiHandler = new Handler(Looper.getMainLooper());

        Intent i = getIntent();
        server   = i.getStringExtra("server");
        port     = i.getStringExtra("port");
        pingHost = i.getStringExtra("ping");
        proto    = i.getStringExtra("proto");
        udpBw    = i.getStringExtra("udpBw");
        dias     = i.getIntExtra("dias", 0);
        horas    = i.getIntExtra("horas", 0);
        minutos  = i.getIntExtra("minutos", 30);
        intMin   = i.getIntExtra("intMin", 0);
        intSec   = i.getIntExtra("intSec", 30);
        testDur  = i.getIntExtra("testDur", 10);

        intervalo = intMin * 60 + intSec;
        if (intervalo < 1) intervalo = 30;

        btnDetener.setOnClickListener(v -> detener());
        btnReporte.setOnClickListener(v -> verReporte());

        capturarInfoWifi();
        iniciarMonitoreo();
    }

    private void capturarInfoWifi() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wi = wm.getConnectionInfo();
            wifiSSID = wi.getSSID().replace("\"", "");
            wifiBSSID = wi.getBSSID();
            wifiRSSI = wi.getRssi();
            wifiFreq = wi.getFrequency();
            wifiLinkSpeed = wi.getLinkSpeed() + " Mbps";
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                int std = wi.getWifiStandard();
                if (std == 6) wifiStandard = "Wi-Fi 6 (802.11ax)";
                else if (std == 5) wifiStandard = "Wi-Fi 5 (802.11ac)";
                else if (std == 4) wifiStandard = "Wi-Fi 4 (802.11n)";
                else wifiStandard = "Wi-Fi std=" + std;
            }
        } catch (Exception e) {
            wifiSSID = "No disponible";
        }
    }

    private void iniciarMonitoreo() {
        running = true;
        startTs = System.currentTimeMillis() / 1000;
        startTime = SDF.format(new Date());

        long totalSeg = (long)dias * 86400 + horas * 3600 + minutos * 60;
        endTs = totalSeg > 0 ? startTs + totalSeg : 0;

        appendLog("════════════════════════════════════");
        appendLog("  MONITOR DE RED - COOPELESCA v1.0");
        appendLog("════════════════════════════════════");
        appendLog("Servidor : " + server + ":" + port);
        appendLog("Ping a   : " + pingHost);
        appendLog("Protocolo: " + proto);
        appendLog("Intervalo: " + intervalo + "s | Test: " + testDur + "s");
        appendLog("Inicio   : " + startTime);
        appendLog("────────────────────────────────────");
        appendLog("WiFi     : " + wifiSSID);
        appendLog("BSSID    : " + wifiBSSID);
        appendLog("RSSI     : " + wifiRSSI + " dBm");
        appendLog("Frecuencia: " + wifiFreq + " MHz");
        appendLog("Estándar : " + wifiStandard);
        appendLog("Link Speed: " + wifiLinkSpeed);
        appendLog("════════════════════════════════════");

        monitorThread = new Thread(() -> {
            while (running) {
                long t0 = System.currentTimeMillis() / 1000;
                iteracion++;
                String ts = SDF.format(new Date());

                uiHandler.post(() -> {
                    tvIter.setText("Prueba: " + iteracion);
                    tvEstado.setText("Ejecutando prueba #" + iteracion + "...");
                });

                double tcpDl=0, tcpUl=0, tcpRDl=0, tcpRUl=0;
                double udpDl=0, udpUl=0, udpJDl=0, udpJUl=0, udpLDl=0, udpLUl=0;
                double pingVal=0, pingLoss=100;

                if (proto.equals("TCP") || proto.equals("AMBOS")) {
                    appendLog("\n[" + ts.substring(11) + "] #" + iteracion + " TCP-DL...");
                    double[] dl = medirTcpDl();
                    tcpDl = dl[0]; tcpRDl = dl[1];
                    appendLog("TCP-UL...");
                    double[] ul = medirTcpUl();
                    tcpUl = ul[0]; tcpRUl = ul[1];
                    appendLog(String.format("TCP: DL=%.2fMbps UL=%.2fMbps Retrans:DL=%.0f UL=%.0f",
                            tcpDl, tcpUl, tcpRDl, tcpRUl));
                }

                if (proto.equals("UDP") || proto.equals("AMBOS")) {
                    appendLog("UDP-DL...");
                    double[] dl = medirUdpDl();
                    udpDl=dl[0]; udpJDl=dl[1]; udpLDl=dl[2];
                    appendLog("UDP-UL...");
                    double[] ul = medirUdpUl();
                    udpUl=ul[0]; udpJUl=ul[1]; udpLUl=ul[2];
                    appendLog(String.format("UDP: DL=%.2fMbps UL=%.2fMbps Jitter:DL=%.3fms UL=%.3fms Loss:DL=%.2f%% UL=%.2f%%",
                            udpDl, udpUl, udpJDl, udpJUl, udpLDl, udpLUl));
                }

                appendLog("Ping...");
                double[] ping = medirPing();
                pingVal=ping[0]; pingLoss=ping[1];
                appendLog(String.format("Ping: %.2fms Loss=%.2f%%", pingVal, pingLoss));

                // Actualizar RSSI en cada prueba
                capturarInfoWifi();

                StringBuilder obj = new StringBuilder("addDataPoint({");
                obj.append("timestamp:'").append(ts).append("'");
                if (proto.equals("TCP") || proto.equals("AMBOS")) {
                    obj.append(String.format(Locale.US, ",tcp_dl:%.2f,tcp_ul:%.2f,tcp_retrans_dl:%.0f,tcp_retrans_ul:%.0f",
                            tcpDl, tcpUl, tcpRDl, tcpRUl));
                }
                if (proto.equals("UDP") || proto.equals("AMBOS")) {
                    obj.append(String.format(Locale.US, ",udp_dl:%.2f,udp_ul:%.2f,udp_jitter_dl:%.3f,udp_jitter_ul:%.3f,udp_loss_dl:%.2f,udp_loss_ul:%.2f",
                            udpDl, udpUl, udpJDl, udpJUl, udpLDl, udpLUl));
                }
                obj.append(String.format(Locale.US, ",ping:%.2f,ping_loss:%.2f,rssi:%d", pingVal, pingLoss, wifiRSSI));
                obj.append("});");
                dataPoints.add(obj.toString());

                long now = System.currentTimeMillis() / 1000;
                if (endTs > 0) {
                    long resta = endTs - now;
                    if (resta <= 0) { running = false; break; }
                    final String tiempoStr = formatTiempo(resta);
                    uiHandler.post(() -> tvTiempoRestante.setText("Resta: " + tiempoStr));
                } else {
                    uiHandler.post(() -> tvTiempoRestante.setText("Continuo"));
                }

                long elapsed = (System.currentTimeMillis() / 1000) - t0;
                long sleep = intervalo - elapsed;
                if (sleep > 0 && running) {
                    try { Thread.sleep(sleep * 1000); } catch (InterruptedException e) { break; }
                }
            }

            uiHandler.post(() -> {
                tvEstado.setText("✅ Completado - " + iteracion + " pruebas");
                generarHtml();
                btnReporte.setEnabled(true);
                btnDetener.setEnabled(false);
            });
        });
        monitorThread.start();
    }

    private double[] medirTcpDl() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{
                    getIperf3Path(), "-c", server, "-p", port, "-R", "-P", "4", "-t",
                    String.valueOf(testDur), "-J"});
            String out = readStream(p.getInputStream());
            p.waitFor();
            double bps = parseJson(out, "sum_received", "bits_per_second");
            double retrans = parseJson(out, "sum_received", "retransmits");
            return new double[]{bps / 1_000_000, retrans};
        } catch (Exception e) { return new double[]{0, 0}; }
    }

    private double[] medirTcpUl() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{
                    getIperf3Path(), "-c", server, "-p", port, "-t",
                    String.valueOf(testDur), "-J"});
            String out = readStream(p.getInputStream());
            p.waitFor();
            double bps = parseJson(out, "sum_sent", "bits_per_second");
            double retrans = parseJson(out, "sum_sent", "retransmits");
            return new double[]{bps / 1_000_000, retrans};
        } catch (Exception e) { return new double[]{0, 0}; }
    }

    private double[] medirUdpDl() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{
                    getIperf3Path(), "-c", server, "-p", port, "-R", "-u",
                    "-b", udpBw, "-t", String.valueOf(testDur), "-J"});
            String out = readStream(p.getInputStream());
            p.waitFor();
            double bps  = parseJsonSum(out, "bits_per_second");
            double jit  = parseJsonSum(out, "jitter_ms");
            double loss = parseJsonSum(out, "lost_percent");
            return new double[]{bps / 1_000_000, jit, loss};
        } catch (Exception e) { return new double[]{0, 0, 0}; }
    }

    private double[] medirUdpUl() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{
                    getIperf3Path(), "-c", server, "-p", port, "-u",
                    "-b", udpBw, "-t", String.valueOf(testDur), "-J"});
            String out = readStream(p.getInputStream());
            p.waitFor();
            double bps  = parseJsonSum(out, "bits_per_second");
            double jit  = parseJsonSum(out, "jitter_ms");
            double loss = parseJsonSum(out, "lost_percent");
            return new double[]{bps / 1_000_000, jit, loss};
        } catch (Exception e) { return new double[]{0, 0, 0}; }
    }

    private double[] medirPing() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"ping", "-c", "5", "-W", "2", pingHost});
            String out = readStream(p.getInputStream());
            p.waitFor();
            double avg = 0, loss = 100;
            for (String line : out.split("\n")) {
                if (line.contains("rtt") && line.contains("/")) {
                    String[] parts = line.split("/");
                    if (parts.length >= 5) avg = Double.parseDouble(parts[4].trim());
                }
                if (line.contains("packet loss")) {
                    String[] parts = line.split(",");
                    for (String part : parts) {
                        if (part.contains("packet loss")) {
                            loss = Double.parseDouble(part.trim().replace("% packet loss", "").trim());
                        }
                    }
                }
            }
            return new double[]{avg, loss};
        } catch (Exception e) { return new double[]{0, 100}; }
    }

    private String getIperf3Path() {
        return getApplicationInfo().nativeLibraryDir + "/libiperf3.so";
    }

    private String readStream(InputStream is) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append("\n");
        return sb.toString();
    }

    private double parseJson(String json, String section, String key) {
        try {
            int si = json.indexOf("\"" + section + "\"");
            if (si < 0) return 0;
            int ki = json.indexOf("\"" + key + "\"", si);
            if (ki < 0) return 0;
            int ci = json.indexOf(":", ki);
            int ei = json.indexOf(",", ci);
            if (ei < 0) ei = json.indexOf("}", ci);
            return Double.parseDouble(json.substring(ci + 1, ei).trim());
        } catch (Exception e) { return 0; }
    }

    private double parseJsonSum(String json, String key) {
        try {
            int si = json.indexOf("\"end\"");
            if (si < 0) return 0;
            int ki = json.indexOf("\"" + key + "\"", si);
            if (ki < 0) return 0;
            int ci = json.indexOf(":", ki);
            int ei = json.indexOf(",", ci);
            if (ei < 0) ei = json.indexOf("}", ci);
            return Double.parseDouble(json.substring(ci + 1, ei).trim());
        } catch (Exception e) { return 0; }
    }

    private String formatTiempo(long seg) {
        long h = seg / 3600, m = (seg % 3600) / 60, s = seg % 60;
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s);
    }

    private void appendLog(String msg) {
        logBuffer.append(msg).append("\n");
        uiHandler.post(() -> {
            tvLog.setText(logBuffer.toString());
            scrollLog.post(() -> scrollLog.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void generarHtml() {
        try {
            InputStream tmpl = getAssets().open("reporte_template.html");
            BufferedReader br = new BufferedReader(new InputStreamReader(tmpl));
            StringBuilder html = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) html.append(line).append("\n");
            tmpl.close();

            String finTime = SDF.format(new Date());
            String contenido = html.toString()
                .replace("SERVIDOR_PLACEHOLDER", server + ":" + port)
                .replace("PING_HOST_PLACEHOLDER", pingHost)
                .replace("PROTOCOLO_PLACEHOLDER", proto)
                .replace("INICIO_PLACEHOLDER", startTime)
                .replace("FIN_PLACEHOLDER", finTime)
                .replace("INTERVALO_PLACEHOLDER", String.valueOf(intervalo))
                .replace("WIFI_SSID_PLACEHOLDER", wifiSSID)
                .replace("WIFI_BSSID_PLACEHOLDER", wifiBSSID)
                .replace("WIFI_RSSI_PLACEHOLDER", wifiRSSI + " dBm")
                .replace("WIFI_FREQ_PLACEHOLDER", wifiFreq + " MHz")
                .replace("WIFI_STD_PLACEHOLDER", wifiStandard)
                .replace("WIFI_SPEED_PLACEHOLDER", wifiLinkSpeed);

            StringBuilder dataSb = new StringBuilder();
            for (String dp : dataPoints) dataSb.append(dp).append("\n");
            contenido = contenido.replace("// DATA_PLACEHOLDER", dataSb.toString());

            File outDir = new File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "Coopelesca");
            outDir.mkdirs();
            String fname = "reporte_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".html";
            File outFile = new File(outDir, fname);
            FileWriter fw = new FileWriter(outFile);
            fw.write(contenido);
            fw.close();
            htmlPath = outFile.getAbsolutePath();
            appendLog("\n📊 Reporte: " + htmlPath);
        } catch (Exception e) {
            appendLog("❌ Error generando reporte: " + e.getMessage());
        }
    }

    private void detener() {
        running = false;
        if (monitorThread != null) monitorThread.interrupt();
        tvEstado.setText("⏹ Detenido");
        generarHtml();
        btnReporte.setEnabled(true);
        btnDetener.setEnabled(false);
    }

    private void verReporte() {
        if (htmlPath.isEmpty()) return;
        Intent i = new Intent(this, ReportActivity.class);
        i.putExtra("htmlPath", htmlPath);
        startActivity(i);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running = false;
        if (monitorThread != null) monitorThread.interrupt();
    }
}
