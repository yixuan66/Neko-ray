package com.neko.hostnamefinder;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Html;
import android.view.View;
import android.widget.*;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.neko.v2ray.R;
import com.neko.v2ray.ui.BaseActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Random;

public class HostnameFinder extends BaseActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_MANAGE_EXTERNAL_STORAGE = 3123;
    
    private ImageView copyButton;
    private EditText ipInput, resultOutput;
    private TextView resultCount;
    private Button scanButton;
    private Handler mHandler;
    private CompoundButton saveLocal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uwu_hostname_finder);
        
        initializeUI();
    }

    private void initializeUI() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        CollapsingToolbarLayout toolbarLayout = findViewById(R.id.collapsing_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ipInput = findViewById(R.id.ip_input);
        resultOutput = findViewById(R.id.result_output);
        resultCount = findViewById(R.id.result_count);
        scanButton = findViewById(R.id.scan_button);
        copyButton = findViewById(R.id.copy_button);
        saveLocal = findViewById(R.id.save_local);
        mHandler = new Handler();

        scanButton.setOnClickListener(view -> scan());
        copyButton.setOnClickListener(view -> copyText(resultOutput.getText().toString()));
        saveLocal.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !requestStoragePermission()) {
                saveLocal.setChecked(false);
            }
        });
    }

    private void scan() {
        String ip = ipInput.getText().toString().trim();
        if (ip.isEmpty()) {
            showToast("Please enter the IP or Hostname first");
            return;
        }

        resultOutput.setText("");
        resultCount.setText("Searching...");
        showToast("Searching...");
        
        new Thread(() -> fetchHostnames(ip)).start();
    }

    private void fetchHostnames(String ip) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://domains.yougetsignal.com/domains.php").openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            connection.setDoOutput(true);
            
            connection.getOutputStream().write(("remoteAddress=" + URLEncoder.encode(ip, "UTF-8")).getBytes("UTF-8"));
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String response = reader.readLine();
            reader.close();
            
            handleResponse(response);
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void handleResponse(String response) {
        mHandler.post(() -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                String status = jsonObject.getString("status");
                
                if ("Fail".equals(status)) {
                    String message = jsonObject.getString("message");
                    resultOutput.setText(message);
                    resultCount.setText(Html.fromHtml("<span style='color:#ff0000'>" + message + "</span>"));
                    return;
                }
                
                JSONArray domainArray = jsonObject.getJSONArray("domainArray");
                StringBuilder resultBuilder = new StringBuilder();
                for (int i = 0; i < domainArray.length(); i++) {
                    resultBuilder.append(domainArray.getString(i).replace("[\"", "").replace("\",\"\"]", "")).append("\n");
                }
                
                resultOutput.setText(resultBuilder.toString());
                resultCount.setText("Results Found: " + jsonObject.getString("domainCount"));

                if (saveLocal.isChecked()) {
                    saveToFile(resultBuilder.toString());
                }
            } catch (Exception e) {
                showError(e.getMessage());
            }
        });
    }

    private void copyText(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setText(text);
            showToast("Results have been copied");
        }
    }

    private boolean requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == 0) {
                return true;
            }
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            return false;
        } else {
            if (Environment.isExternalStorageManager()) {
                return true;
            }
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE);
            return false;
        }
    }

    private void saveToFile(String content) {
        File directory = new File(Environment.getExternalStorageDirectory(), "Hostname Finder");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, String.format("%05d.txt", new Random().nextInt(100000)));
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(content);
            showToast("Saved in: " + file.getPath());
        } catch (IOException e) {
            showError(e.getMessage());
        }
    }

    private void showToast(String message) {
        mHandler.post(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void showError(String error) {
        mHandler.post(() -> {
            resultOutput.setText(error);
            resultCount.setText("");
        });
    }
}
