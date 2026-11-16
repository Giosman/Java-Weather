package com.example.demo1;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Hämtar prognostiserad temperatur från MET Norway. */
public final class WeatherApi implements AutoCloseable {
    private static final String DEFAULT_FORECAST_URL =
            "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=62.39&lon=17.31";
    // Byt till projektets publika kontaktsida om adressen ändras.
    private static final String USER_AGENT =
            "Java-Weather/1.0 https://github.com/orbeg/Java-Weather";

    private static String cachedUrl;
    private static Double cachedTemperature;
    private static long expiresAt;
    private static long lastModified;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean closed;

    public interface Callback {
        void onSuccess(double temperature);
        void onError(Exception error);
    }

    /** Hämtar väder för standardplatsen (Sundsvall). */
    public void fetchWeather(Callback callback) {
        fetchWeather(DEFAULT_FORECAST_URL, callback);
    }

    /** Nätverksanropet körs i bakgrunden med angiven URL. Resultatet levereras på UI-tråden. */
    public void fetchWeather(String forecastUrl, Callback callback) {
        if (closed) {
            return;
        }
        executor.execute(() -> {
            try {
                double temperature = fetchTemperature(forecastUrl);
                mainHandler.post(() -> {
                    if (!closed) {
                        callback.onSuccess(temperature);
                    }
                });
            } catch (Exception error) {
                mainHandler.post(() -> {
                    if (!closed) {
                        callback.onError(error);
                    }
                });
            }
        });
    }

    private static synchronized double fetchTemperature(String forecastUrl) throws Exception {
        if (Objects.equals(forecastUrl, cachedUrl) && cachedTemperature != null && System.currentTimeMillis() < expiresAt) {
            return cachedTemperature;
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(forecastUrl).openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        if (Objects.equals(forecastUrl, cachedUrl) && cachedTemperature != null && lastModified > 0) {
            connection.setIfModifiedSince(lastModified);
        }

        try {
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_NOT_MODIFIED && cachedTemperature != null) {
                expiresAt = connection.getExpiration();
                return cachedTemperature;
            }
            if (status != HttpURLConnection.HTTP_OK
                    && status != HttpURLConnection.HTTP_NOT_AUTHORITATIVE) {
                throw new IOException("Väder-API svarade med HTTP " + status);
            }

            StringBuilder json = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }
            }

            JSONArray forecasts = new JSONObject(json.toString())
                    .getJSONObject("properties").getJSONArray("timeseries");
            if (forecasts.length() == 0) {
                throw new IOException("Väderprognosen är tom");
            }
            double temperature = forecasts.getJSONObject(0)
                    .getJSONObject("data")
                    .getJSONObject("instant")
                    .getJSONObject("details")
                    .getDouble("air_temperature");

            cachedUrl = forecastUrl;
            cachedTemperature = temperature;
            expiresAt = connection.getExpiration();
            lastModified = connection.getLastModified();
            return temperature;
        } finally {
            connection.disconnect();
        }
    }

    @Override
    public void close() {
        closed = true;
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
    }
}
