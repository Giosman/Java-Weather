package com.example.demo1;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import java.util.Locale;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private final WeatherApi weatherApi = new WeatherApi();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView temperatureView = findViewById(R.id.tvTemperature);
        temperatureView.setText("Hämtar väder…");
        weatherApi.fetchWeather(new WeatherApi.Callback() {
            @Override
            public void onSuccess(double temperature) {
                temperatureView.setText(String.format(
                        Locale.getDefault(), "%.1f °C", temperature));
            }

            @Override
            public void onError(Exception error) {
                temperatureView.setText("Kunde inte hämta väder");
                Log.e("WeatherApi", "Kunde inte hämta prognosen", error);
            }
        });
    }

    @Override
    protected void onDestroy() {
        weatherApi.close();
        super.onDestroy();
    }
}
