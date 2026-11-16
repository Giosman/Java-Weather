package com.example.demo1;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class
MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private LocationManager locationManager;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (Boolean.TRUE.equals(fineGranted) || Boolean.TRUE.equals(coarseGranted)) {
                    fetchWeatherUrl();
                } else {
                    Toast.makeText(this, "Platsbehörighet krävs för att hämta väder baserat på din position", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationManager = new LocationManager(this);
        requestLocationAndFetchUrl();
    }

    private void requestLocationAndFetchUrl() {
        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void fetchWeatherUrl() {
        locationManager.getWeatherApiUrl(new LocationManager.WeatherUrlCallback() {
            @Override
            public void onUrlReady(String url) {
                Log.d(TAG, "Anropar MET API med URL: " + url);
                // url innehåller nu t.ex.:
                // https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=62.3900&lon=17.3100
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Kunde inte hämta plats/URL: " + error);
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
