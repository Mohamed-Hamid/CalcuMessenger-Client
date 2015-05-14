package com.bitsplease.calcumessenger;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class WelcomeActivity extends Activity {

    EditText ip, port;
    Button enter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_welcome);

        ip = (EditText) findViewById(R.id.ip);
        port = (EditText) findViewById(R.id.port);
        enter = (Button) findViewById(R.id.enter_button);

        enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String givenIp = ip.getText().toString();
                String givenPort = port.getText().toString();

                if (givenIp != "" && givenPort != "") {

                    Intent intent = new Intent(WelcomeActivity.this,
                            MainActivity.class);
                    intent.putExtra("ip", givenIp);
                    intent.putExtra("port", givenPort);

                    startActivity(intent);

                } else {
                    Toast.makeText(getApplicationContext(),
                            "Please Enter IP and Port", Toast.LENGTH_LONG).show();
                }
            }
        });

    }
}
