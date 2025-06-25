package com.example.mycaptureapp;

// A LOT of imports are needed for this file!
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class CaptureService extends Service {

    public static final String ACTION_START = "com.example.mycaptureapp.action.START";
    public static final String ACTION_STOP = "com.example.mycaptureapp.action.STOP";
    public static final String EXTRA_RESULT_CODE = "resultCode";
    public static final String EXTRA_RESULT_DATA = "resultData";

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private MediaRecorder mediaRecorder;
    private Handler handler;
    private Timer timer;
    private int captureCount;
    private int currentCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_START.equals(intent.getAction())) {
            createNotificationChannel();
            Notification notification = new Notification.Builder(this, "capture_channel")
                    .setContentTitle("Capture Service")
                    .setContentText("Actively capturing screen and audio.")
                    .setSmallIcon(android.R.drawable.ic_menu_camera)
                    .build();
            startForeground(1, notification);

            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1);
            Intent resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
            captureCount = intent.getIntExtra("CAPTURE_COUNT", 0);
            long period = intent.getIntExtra("CAPTURE_PERIOD", 60) * 1000L;
            currentCount = 0;

            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData);
            startCaptureLoop(period);
        } else if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopCapture();
        }
        return START_NOT_STICKY;
    }

    private void startCaptureLoop(long period) {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (currentCount >= captureCount) {
                    stopCapture();
                    return;
                }
                handler.post(() -> {
                    captureScreenshot();
                    captureAudio();
                });
                currentCount++;
            }
        }, 0, period);
    }

    private void captureScreenshot() {
        if (mediaProjection == null) return;

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int density = metrics.densityDpi;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);

        imageReader.setOnImageAvailableListener(reader -> {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * width;

                    Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);

                    // Crop the padding out
                    Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
                    uploadBitmap(croppedBitmap);

                    bitmap.recycle();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (image != null) {
                    image.close();
                }
                if (virtualDisplay != null) {
                    virtualDisplay.release();
                }
                if (imageReader != null) {
                   imageReader.close();
                }
            }
        }, handler);
    }

    private void captureAudio() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
        String fileName = getExternalCacheDir().getAbsolutePath() + "/" + timestamp + ".3gp";

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(fileName);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Stop recording after 5 seconds and upload
        new Handler().postDelayed(() -> {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            uploadAudio(fileName);
        }, 5000); // 5 seconds of audio
    }


    private void uploadBitmap(Bitmap bitmap) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
        StorageReference imageRef = storageRef.child("captures/" + timestamp + ".jpg");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] data = baos.toByteArray();

        imageRef.putBytes(data);
    }

    private void uploadAudio(String filePath) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        File file = new File(filePath);
        StorageReference audioRef = storageRef.child("captures/" + file.getName());

        audioRef.putFile(Uri.fromFile(file)).addOnSuccessListener(taskSnapshot -> file.delete());
    }

    private void stopCapture() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        stopForeground(true);
        stopSelf();
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "capture_channel",
                    "Capture Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCapture();
    }
}
