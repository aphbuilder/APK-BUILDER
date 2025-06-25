package com.example.mycaptureapp;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText etCount, etPeriod;
    private Button btnStart, btnStop;
    private MediaProjectionManager mediaProjectionManager;

    private final ActivityResultLauncher<Intent> screenCaptureLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent intent = new Intent(MainActivity.this, CaptureService.class);
                        intent.setAction(CaptureService.ACTION_START);
                        intent.putExtra(CaptureService.EXTRA_RESULT_CODE, result.getResultCode());
                        intent.putExtra(CaptureService.EXTRA_RESULT_DATA, result.getData());
                        intent.putExtra("CAPTURE_COUNT", Integer.parseInt(etCount.getText().toString()));
                        intent.putExtra("CAPTURE_PERIOD", Integer.parseInt(etPeriod.getText().toString()));

                        startForegroundService(intent);
                        Toast.makeText(MainActivity.this, "Capture service started.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Screen capture permission was denied.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etCount = findViewById(R.id.et_capture_count);
        etPeriod = findViewById(R.id.et_period_seconds);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);

        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (etCount.getText().toString().isEmpty() || etPeriod.getText().toString().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                    return;
                }
                // This will trigger the onActivityResult callback
                screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent());
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CaptureService.class);
                intent.setAction(CaptureService.ACTION_STOP);
                startService(intent);
                Toast.makeText(MainActivity.this, "Capture service stopped.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
