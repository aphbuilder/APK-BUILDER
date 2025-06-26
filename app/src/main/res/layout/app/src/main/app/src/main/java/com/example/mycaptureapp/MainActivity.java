package com.example.mycaptureapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText etCount, etPeriod;
    private MediaProjectionManager mediaProjectionManager;
    private static final int PERMISSIONS_REQUEST_CODE = 101;

    private final ActivityResultLauncher<Intent> screenCaptureLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent intent = new Intent(MainActivity.this, CaptureService.class);
                    intent.setAction(CaptureService.ACTION_START);
                    intent.putExtra(CaptureService.EXTRA_RESULT_CODE, result.getResultCode());
                    intent.putExtra(CaptureService.EXTRA_RESULT_DATA, result.getData()); // This line is correct as is for this launcher
                    intent.putExtra("CAPTURE_COUNT", Integer.parseInt(etCount.getText().toString()));
                    intent.putExtra("CAPTURE_PERIOD", Integer.parseInt(etPeriod.getText().toString()));

                    startForegroundService(intent);
                    Toast.makeText(MainActivity.this, "Capture service started.", Toast.LENGTH_SHORT).show();
                    finish(); // Close the app UI
                } else {
                    Toast.makeText(MainActivity.this, "Screen capture permission was denied.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etCount = findViewById(R.id.et_capture_count);
        etPeriod = findViewById(R.id.et_period_seconds);
        Button btnStart = findViewById(R.id.btn_start);
        Button btnStop = findViewById(R.id.btn_stop);

        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        btnStart.setOnClickListener(v -> {
            if (checkAndRequestPermissions()) {
                startCapture();
            }
        });

        btnStop.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CaptureService.class);
            intent.setAction(CaptureService.ACTION_STOP);
            startService(intent);
            Toast.makeText(MainActivity.this, "Capture service stopped.", Toast.LENGTH_SHORT).show();
        });
    }

    private void startCapture() {
        if (etCount.getText().toString().isEmpty() || etPeriod.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }
        // This will trigger the onActivityResult callback
        screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent());
    }

    private boolean checkAndRequestPermissions() {
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean allPermissionsGranted = true;
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false;
                        break;
                    }
                }
                if (allPermissionsGranted) {
                    // Permissions were granted, now we can start the capture
                    startCapture();
                } else {
                    Toast.makeText(this, "Permissions must be granted to run the service.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
