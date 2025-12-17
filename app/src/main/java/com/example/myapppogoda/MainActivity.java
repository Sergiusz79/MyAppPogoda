package com.example.myapppogoda;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText user_field;
    private Button main_btn;
    private TextView result_info;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        user_field = findViewById(R.id.user_field);
        main_btn = findViewById(R.id.main_btn);
        result_info = findViewById(R.id.result_info);

        main_btn.setOnClickListener(view -> {
            String city = user_field.getText().toString().trim();
            if (city.isEmpty()) {
                Toast.makeText(MainActivity.this, R.string.user_no_input, Toast.LENGTH_SHORT).show();
                return;
            }

            String apiKey = "3d6d2ffdbf543bf5bc21fd59ee42cfd4";
            String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city +
                    "&appid=" + apiKey + "&lang=pl&units=metric";

            fetchWeather(url);
        });
    }

    private void fetchWeather(String urlString) {
        result_info.setText("Downloading...");

        executor.execute(() -> {
            String response = null;

            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();

                int responseCode = connection.getResponseCode();
                InputStream inputStream = (responseCode == HttpURLConnection.HTTP_OK) ?
                        connection.getInputStream() : connection.getErrorStream();

                reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                response = result.toString();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) connection.disconnect();
                try {
                    if (reader != null) reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            String finalResponse = response;
            handler.post(() -> updateUI(finalResponse));
        });
    }

    private void updateUI(String response) {
        if (response == null) {
            result_info.setText("Błąd pobierania danych.");
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(response);

            // Проверка на ошибки API
            if (jsonObject.has("cod") && jsonObject.getInt("cod") != 200) {
                String message = jsonObject.has("message") ? jsonObject.getString("message") : "Nieznany błąd";
                result_info.setText("Błąd: " + message);
                return;
            }

            String temp = jsonObject.getJSONObject("main").getString("temp");
            String desc = jsonObject.getJSONArray("weather").getJSONObject(0).getString("description");

            result_info.setText("Temperatura: " + temp + " °C\n" + desc);

        } catch (Exception e) {
            e.printStackTrace();
            result_info.setText("Błąd parsowania danych.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow(); // закрываем executor при уничтожении Activity
    }
}
