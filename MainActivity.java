package com.apk.maxai;

import android.Manifest;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private WebView web;
    private ValueCallback<Uri[]> filePicker;
    private static final String[] PERMS = { "android.permission.CAMERA", "android.permission.RECORD_AUDIO", "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION", "android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO", "android.permission.READ_MEDIA_AUDIO", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS", "android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR", "android.permission.GET_ACCOUNTS", "android.permission.READ_PHONE_STATE", "android.permission.READ_PHONE_NUMBERS", "android.permission.CALL_PHONE", "android.permission.ANSWER_PHONE_CALLS", "android.permission.BODY_SENSORS", "android.permission.ACTIVITY_RECOGNITION", "android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN", "android.permission.NEARBY_WIFI_DEVICES", "android.permission.POST_NOTIFICATIONS" };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setGeolocationEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(false);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, android.webkit.WebResourceRequest r) {
                String u = r.getUrl().toString();
                if (u.startsWith("http") || u.startsWith("file:")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); } catch (Exception e) { }
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest r) {
                runOnUiThread(() -> r.grant(r.getResources()));
            }
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb) {
                cb.invoke(origin, true, false);
            }
            @Override
            public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (filePicker != null) filePicker.onReceiveValue(null);
                filePicker = cb;
                try { startActivityForResult(p.createIntent(), 1); }
                catch (Exception e) { filePicker = null; return false; }
                return true;
            }
        });
        web.setDownloadListener((url, ua, cd, mime, len) -> {
            try {
                DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
                r.setMimeType(mime);
                r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, cd, mime));
                ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(r);
            } catch (Exception e) { }
        });
        if (PERMS.length > 0) ActivityCompat.requestPermissions(this, PERMS, 100);
        web.loadUrl("file:///android_asset/www/index.html");
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 1 && filePicker != null) {
            filePicker.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            filePicker = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    private boolean has(String p) {
        return ContextCompat.checkSelfPermission(this, "android.permission." + p) == PackageManager.PERMISSION_GRANTED;
    }

    public class Bridge {
        @JavascriptInterface public void toast(final String t) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, t, Toast.LENGTH_SHORT).show());
        }
        @JavascriptInterface public void vibrate(long ms) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        }
        @JavascriptInterface public void flashlight(boolean on) {
            try {
                CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                cm.setTorchMode(cm.getCameraIdList()[0], on);
            } catch (Exception e) { }
        }
        @JavascriptInterface public void share(String t) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, t);
            startActivity(Intent.createChooser(i, null));
        }
        @JavascriptInterface public void copy(final String t) {
            runOnUiThread(() -> ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("t", t)));
        }
        @JavascriptInterface public void openUrl(String u) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u)));
        }
        @JavascriptInterface public void call(String n) {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + n)));
        }
        @JavascriptInterface public void sms(String n, String m) {
            Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + n));
            i.putExtra("sms_body", m);
            startActivity(i);
        }
        @JavascriptInterface public void showNotification(String t, String m) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel("app", "App", NotificationManager.IMPORTANCE_DEFAULT));
            nm.notify(1, new Notification.Builder(MainActivity.this, "app").setContentTitle(t).setContentText(m).setSmallIcon(getApplicationInfo().icon).build());
        }
        @JavascriptInterface public void keepScreenOn(final boolean on) {
            runOnUiThread(() -> {
                if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
        }
        @JavascriptInterface public void setBrightness(final float v) {
            runOnUiThread(() -> {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.screenBrightness = v;
                getWindow().setAttributes(lp);
            });
        }
        @JavascriptInterface public String deviceInfo() {
            try {
                JSONObject o = new JSONObject();
                o.put("model", Build.MODEL);
                o.put("brand", Build.BRAND);
                o.put("manufacturer", Build.MANUFACTURER);
                o.put("android", Build.VERSION.RELEASE);
                o.put("sdk", Build.VERSION.SDK_INT);
                return o.toString();
            } catch (Exception e) { return "{}"; }
        }
        @JavascriptInterface public String battery() {
            try {
                BatteryManager bm = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
                JSONObject o = new JSONObject();
                o.put("level", bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));
                o.put("charging", bm.isCharging());
                return o.toString();
            } catch (Exception e) { return "{}"; }
        }
        @JavascriptInterface public String getLocation() {
            try {
                if (!has("ACCESS_FINE_LOCATION") && !has("ACCESS_COARSE_LOCATION")) return "{}";
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                Location l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (l == null) l = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (l == null) return "{}";
                JSONObject o = new JSONObject();
                o.put("lat", l.getLatitude());
                o.put("lng", l.getLongitude());
                o.put("accuracy", l.getAccuracy());
                return o.toString();
            } catch (Exception e) { return "{}"; }
        }
        @JavascriptInterface public boolean hasPermission(String p) { return has(p); }
        @JavascriptInterface public void requestPermission(String p) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{ "android.permission." + p }, 101);
        }
        @JavascriptInterface public void exit() { finishAffinity(); }
    }
}
