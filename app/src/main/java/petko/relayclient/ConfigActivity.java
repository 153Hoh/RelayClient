package petko.relayclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import relay.petko.relay.log.CustomLogger;
import relay.petko.relay.log.Logging;

public class ConfigActivity extends AppCompatActivity {

    Spinner spinner;
    EditText serverAddressText;
    Button startBut;

    private static final CustomLogger log = Logging.getLogger(ConfigActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        spinner = (Spinner) findViewById(R.id.choose);
        List<String> categories = new ArrayList<>();
        categories.add("Eszköz kezelő");
        categories.add("Írányító");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);

        serverAddressText = (EditText) findViewById(R.id.server_address);
        serverAddressText.setText(BuildConfig.SERVER_ADDRESS);

        startBut = (Button) findViewById(R.id.startBut);
        startBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String serverAddress = serverAddressText.getText().toString();
                int type = spinner.getSelectedItemPosition();
                log.debug(type);
                SharedPreferences.Editor editor = RelayClientApplication.config.edit();
                editor.putString(RelayClientApplication.PreferenceKeys.SERVER_ADDRESS,serverAddress);
                switch (type){
                    case 0:
                        editor.putString(RelayClientApplication.PreferenceKeys.DEVICE_TYPE,"handler");
                        Intent handlerIntent = new Intent(ConfigActivity.this,HandlerActivity.class);
                        startActivity(handlerIntent);
                        break;
                    case 1:
                        editor.putString(RelayClientApplication.PreferenceKeys.DEVICE_TYPE,"controller");
                        Intent controllerIntent = new Intent(ConfigActivity.this,LoginToControllerActivity.class);
                        startActivity(controllerIntent);
                        break;
                }
                editor.apply();
            }
        });
    }
}
