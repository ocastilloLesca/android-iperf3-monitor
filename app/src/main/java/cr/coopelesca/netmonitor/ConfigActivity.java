package cr.coopelesca.netmonitor;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.*;

public class ConfigActivity extends Activity {

    private EditText etServer, etPort, etPing, etUdpBw;
    private EditText etDias, etHoras, etMinutos;
    private EditText etIntMin, etIntSec, etTestDur;
    private Spinner spinnerProto;
    private View labelUdpBw;
    private Button btnIniciar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        etServer   = findViewById(R.id.etServer);
        etPort     = findViewById(R.id.etPort);
        etPing     = findViewById(R.id.etPing);
        etUdpBw    = findViewById(R.id.etUdpBw);
        etDias     = findViewById(R.id.etDias);
        etHoras    = findViewById(R.id.etHoras);
        etMinutos  = findViewById(R.id.etMinutos);
        etIntMin   = findViewById(R.id.etIntMin);
        etIntSec   = findViewById(R.id.etIntSec);
        etTestDur  = findViewById(R.id.etTestDur);
        spinnerProto = findViewById(R.id.spinnerProto);
        labelUdpBw = findViewById(R.id.labelUdpBw);
        btnIniciar = findViewById(R.id.btnIniciar);

        String[] protos = {"TCP", "UDP", "AMBOS"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, protos);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProto.setAdapter(adapter);
        spinnerProto.setSelection(2);

        spinnerProto.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                boolean showUdp = pos != 0;
                labelUdpBw.setVisibility(showUdp ? View.VISIBLE : View.GONE);
                etUdpBw.setVisibility(showUdp ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        btnIniciar.setOnClickListener(v -> {
            if (!validar()) return;
            Intent i = new Intent(this, MonitorActivity.class);
            i.putExtra("server",   etServer.getText().toString().trim());
            i.putExtra("port",     etPort.getText().toString().trim());
            i.putExtra("ping",     etPing.getText().toString().trim());
            i.putExtra("proto",    spinnerProto.getSelectedItem().toString());
            i.putExtra("udpBw",    etUdpBw.getText().toString().trim());
            i.putExtra("dias",     intVal(etDias));
            i.putExtra("horas",    intVal(etHoras));
            i.putExtra("minutos",  intVal(etMinutos));
            i.putExtra("intMin",   intVal(etIntMin));
            i.putExtra("intSec",   intVal(etIntSec));
            i.putExtra("testDur",  intVal(etTestDur));
            startActivity(i);
        });
    }

    private int intVal(EditText et) {
        try { return Integer.parseInt(et.getText().toString().trim()); }
        catch (Exception e) { return 0; }
    }

    private boolean validar() {
        if (etServer.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Ingresá la IP del servidor", Toast.LENGTH_SHORT).show();
            return false;
        }
        int port = intVal(etPort);
        if (port < 1 || port > 65535) {
            Toast.makeText(this, "Puerto inválido", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etPing.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Ingresá la IP de Ping", Toast.LENGTH_SHORT).show();
            return false;
        }
        int intervalo = intVal(etIntMin) * 60 + intVal(etIntSec);
        if (intervalo < 1) {
            Toast.makeText(this, "Intervalo mínimo: 1 segundo", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (intVal(etTestDur) < 1) {
            Toast.makeText(this, "Duración mínima: 1 segundo", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
