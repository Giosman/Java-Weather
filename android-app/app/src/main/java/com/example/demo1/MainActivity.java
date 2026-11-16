package com.example.demo1;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private LocationManager locationManager;
    private final WeatherApi weatherApi = new WeatherApi();
    private TextView temperatureView;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (Boolean.TRUE.equals(fineGranted) || Boolean.TRUE.equals(coarseGranted)) {
                    fetchWeatherForCurrentLocation();
                } else {
                    Toast.makeText(this, "Platsbehörighet krävs för att hämta väder baserat på din position", Toast.LENGTH_SHORT).show();
                    temperatureView.setText("Platsbehörighet saknas");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        temperatureView = findViewById(R.id.tvTemperature);
        locationManager = new LocationManager(this);

        requestLocationAndFetchWeather();
    }

    private void requestLocationAndFetchWeather() {
        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void fetchWeatherForCurrentLocation() {
        temperatureView.setText("Hämtar plats…");
        locationManager.getWeatherApiUrl(new LocationManager.WeatherUrlCallback() {
            @Override
            public void onUrlReady(String url) {
                Log.d(TAG, "Anropar MET API med URL: " + url);
                temperatureView.setText("Hämtar väder…");

                weatherApi.fetchWeather(url, new WeatherApi.Callback() {
                    @Override
                    public void onSuccess(double temperature) {
                        temperatureView.setText(String.format(
                                Locale.getDefault(), "%.1f °C", temperature));
                    }

                    @Override
                    public void onError(Exception error) {
                        temperatureView.setText("Kunde inte hämta väder");
                        Log.e(TAG, "Kunde inte hämta prognosen", error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Kunde inte hämta plats/URL: " + error);
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                temperatureView.setText("Kunde inte hämta plats");
            }
        });
    }

    @Override
    protected void onDestroy() {
        weatherApi.close();
        super.onDestroy();
    }
}
