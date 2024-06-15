package com.example.a24b10234_lior_zalta_311621403;

import static java.lang.Thread.sleep;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import android.Manifest;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

public class MainActivity extends AppCompatActivity {

    private MaterialTextView main_LBL_error;
    private TextInputEditText main_ET_password;
    private MaterialButton main_BTN_submit;
    MaterialTextView main_LBL_password;
    private boolean isFlashlightOn = false;
    ActivityResultLauncher<Intent> appSettingsResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
                checkLocationStatus();
            });

    ActivityResultLauncher<String> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts
                            .RequestPermission(), result -> {
                        if (result) {
                            // location access granted.
                            checkLocationStatus();
                        } else {
                            // No location access granted.
                            String permission = checkLocationPermissionsStatus(this);
                            if (permission != null && ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                                buildAlertMessageManuallyPermission(permission);
                            } else {
                                buildAlertMessageManuallyPermission(
                                        checkLocationPermissionsStatus(this)
                                );
                            }
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();
        initViews();
    }

    private void initViews() {
        main_BTN_submit.setOnClickListener(v -> validatePassword());
        getFlashLightStatus();
        checkLocationStatus();
    }

    private void findViews() {
        main_ET_password = findViewById(R.id.main_ET_password);
        main_BTN_submit = findViewById(R.id.main_BTN_submit);
        main_LBL_error = findViewById(R.id.main_LBL_error);
        main_LBL_password = findViewById(R.id.main_LBL_password);
    }

    private void checkLocationStatus() {
        String permissionStatus = checkLocationPermissionsStatus(this);
        if (!isLocationEnabled(this))
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        else if (permissionStatus != null)
            askForLocationPermissions(checkLocationPermissionsStatus(this));
        else {
            validateLocationSensorsEnabled();
        }
    }

    private String checkLocationPermissionsStatus(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return Manifest.permission.ACCESS_FINE_LOCATION;
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return Manifest.permission.ACCESS_COARSE_LOCATION;
        return null;
    }

    private boolean isLocationEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return lm.isLocationEnabled();
        } else {
            int mode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
            return mode != Settings.Secure.LOCATION_MODE_OFF;
        }
    }

    private void askForLocationPermissions(String permission) {
        if (shouldShowRequestPermissionRationale(permission)) {
            if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                buildAlertMessageManuallyPermission(permission);
            else
                locationPermissionRequest.launch(permission);
        } else {
            locationPermissionRequest.launch(permission);
        }
    }

    private void buildAlertMessageManuallyPermission(String permission) {
        if (permission == null) return;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String allow_message_type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? "Allow all the time" : "Allow";

        builder.setMessage("You need to enable location permission manually." +
                        "\nOn the page that opens - click on PERMISSIONS, then on LOCATION and then check '" + allow_message_type + "'")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> openAppSettings())
                .setNegativeButton("Exit", (dialog, which) -> finish());
        builder.create().show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appSettingsResultLauncher.launch(intent);
    }

    private void validateLocationSensorsEnabled() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        LocationRequest.Builder requestBuilder = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY);
        builder.addLocationRequest(requestBuilder.setPriority(Priority.PRIORITY_HIGH_ACCURACY).build())
                .addLocationRequest(requestBuilder.setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY).build());
        builder.setNeedBle(true);

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);

        settingsClient.checkLocationSettings(builder.build())
                .addOnSuccessListener(locationSettingsResponse -> {
                })
                .addOnFailureListener(e -> Log.e("GPS", "Unable to execute request."))
                .addOnCanceledListener(() -> Log.e("GPS", "checkLocationSettings -> onCanceled"));
    }

    private void validatePassword() {
        String[] passwordParts = main_ET_password.getText().toString().trim().split("_");

        if (passwordParts.length != 2) {
            main_LBL_error.setText("Invalid password: must be in this structure: [battery level]_[wifi ssid]");
            return;
        }
        try {
            getFlashLightStatus();
            Log.d("Battery level", "getBatteryLevel: " + getBatteryLevel());
            Log.d("Wifi ssid", "getWifiSsid: " + getWifiSsid(this).replace("\"", ""));
            Log.d("Arr", "arr[0]: " + passwordParts[0] + " arr[1]: " + passwordParts[1]);

            boolean validBatteryLevel = Integer.parseInt(passwordParts[0]) == getBatteryLevel();
            Log.d("Battery level", "validBatteryLevel: " + validBatteryLevel);
            boolean validWifiSSID = getWifiSsid(this).replace("\"", "").equals(passwordParts[1]);
            Log.d("Wifi ssid", "validWifiSSID: " + validWifiSSID);

            if (getWifiSsid(this) == null)
                main_LBL_error.setText("Not connected to wifi");
            else if (!isFlashlightOn)
                main_LBL_error.setText("Flashlight is off - turn it ON and press Submit again");
            else if (!validBatteryLevel || !validWifiSSID)
                main_LBL_error.setText("wrong password: no match to battery level or wifi ssid");
            else
                LoginSuccess();
        } catch (Exception e) {
            main_LBL_error.setText("Invalid password");
        }
    }

    private int getBatteryLevel() {
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    private String getWifiSsid(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (connectivityManager != null && wifiManager != null) {
            Network currentNetwork = connectivityManager.getActiveNetwork();
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(currentNetwork);

            if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                return wifiInfo.getSSID(); // Might be enclosed in double quotes
            }
        }

        return null; // Not connected to Wi-Fi
    }

    private void getFlashLightStatus() {
        CameraManager cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        cameraManager.registerTorchCallback(new CameraManager.TorchCallback() {
            @Override
            public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
                super.onTorchModeChanged(cameraId, enabled);
                isFlashlightOn = enabled;
            }
        }, null);
    }

    private void LoginSuccess() {
        main_ET_password.setVisibility(View.INVISIBLE);
        main_BTN_submit.setVisibility(View.INVISIBLE);
        main_LBL_error.setVisibility(View.INVISIBLE);
        main_LBL_password.setTextColor(ContextCompat.getColor(this, R.color.green_800));
        main_LBL_password.setText("Success!!!");
        main_LBL_password.setTextSize(1,30);
    }
}