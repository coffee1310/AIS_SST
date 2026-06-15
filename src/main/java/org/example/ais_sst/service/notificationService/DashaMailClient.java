package org.example.ais_sst.service.notificationService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DashaMailClient {

    @Value("${dashamail.api.key}")
    private String apiKey;

    @Value("${dashamail.api.url:https://api.dashamail.com/}")
    private String baseUrl;

    @Value("${dashamail.timeout:30}")
    private int timeout;

    private OkHttpClient httpClient;
    private Gson gson;


    public DashaMailClient(String apiKey, String apiUrl, OkHttpClient okHttpClient) {
        this.apiKey = System.getenv("DASHAMAIL_API_KEY");
        this.baseUrl = "https://api.dashamail.com/";
        this.httpClient = new OkHttpClient();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @PostConstruct
    public void init() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    // Основной метод для всех запросов
    public Response makeRequest(String method, String endpoint, Object body) throws IOException {
        String url = baseUrl + endpoint;

        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("X-Dashamail-Api-Key", apiKey)
                .addHeader("Content-Type", "application/json");

        switch (method.toUpperCase()) {
            case "GET":
                builder.get();
                break;
            case "POST":
                String jsonBody = body != null ? gson.toJson(body) : "{}";
                RequestBody requestBody = RequestBody.create(
                        jsonBody, MediaType.parse("application/json")
                );
                builder.post(requestBody);
                break;
            case "PUT":
                String jsonBodyPut = body != null ? gson.toJson(body) : "{}";
                RequestBody requestBodyPut = RequestBody.create(
                        jsonBodyPut, MediaType.parse("application/json")
                );
                builder.put(requestBodyPut);
                break;
            case "DELETE":
                if (body != null) {
                    String jsonBodyDelete = gson.toJson(body);
                    RequestBody requestBodyDelete = RequestBody.create(
                            jsonBodyDelete, MediaType.parse("application/json")
                    );
                    builder.delete(requestBodyDelete);
                } else {
                    builder.delete();
                }
                break;
        }

        Request request = builder.build();
        log.debug("Making {} request to: {}", method, url);

        Response response = httpClient.newCall(request).execute();

        if (!response.isSuccessful()) {
            String errorBody = response.body() != null ? response.body().string() : "";
            log.error("API error: {} - {}", response.code(), errorBody);
            response.close();
            throw new IOException("API request failed: " + response.code() + " - " + errorBody);
        }

        return response;
    }

    // Метод для получения списка рассылок (возвращает String, а не объекты)
    public String getCampaignsAsJson() throws IOException {
        Response response = makeRequest("GET", "campaigns/list", null);
        String json = response.body().string();
        response.close();
        return json;
    }

    // Метод для получения списка рассылок (возвращает List<Map>)
    public List<Map<String, Object>> getCampaigns() throws IOException {
        Response response = makeRequest("GET", "campaigns/list", null);
        String json = response.body().string();
        response.close();

        com.google.gson.reflect.TypeToken<List<Map<String, Object>>> typeToken =
                new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>() {};
        return gson.fromJson(json, typeToken.getType());
    }

    // Метод для получения конкретной рассылки
    public Map<String, Object> getCampaign(String campaignId) throws IOException {
        Response response = makeRequest("GET", "campaigns/" + campaignId, null);
        String json = response.body().string();
        response.close();

        com.google.gson.reflect.TypeToken<Map<String, Object>> typeToken =
                new com.google.gson.reflect.TypeToken<Map<String, Object>>() {};
        return gson.fromJson(json, typeToken.getType());
    }

    // Метод для добавления подписчика
    public void addSubscriber(String listId, String email, String name) throws IOException {
        Map<String, Object> data = new HashMap<>();
        data.put("list_id", listId);
        data.put("email", email);
        if (name != null && !name.isEmpty()) {
            data.put("name", name);
        }

        Response response = makeRequest("POST", "subscribers/add", data);
        response.close();
        log.info("Subscriber added: {}", email);
    }

    // Метод для удаления подписчика
    public void deleteSubscriber(String listId, String email) throws IOException {
        Map<String, Object> data = new HashMap<>();
        data.put("list_id", listId);
        data.put("email", email);

        Response response = makeRequest("DELETE", "subscribers/delete", data);
        response.close();
        log.info("Subscriber deleted: {}", email);
    }

    // Метод для отправки письма
    public void sendEmail(String to, String subject, String htmlContent) throws IOException {
        sendEmail(to, subject, htmlContent, null, null);
    }

    public void sendEmail(String to, String subject, String htmlContent, String fromEmail, String fromName) throws IOException {
        Map<String, Object> data = new HashMap<>();
        data.put("to", to);
        data.put("subject", subject);
        data.put("html", htmlContent);
        data.put("from_email", fromEmail != null ? fromEmail : "noreply@yourdomain.com");
        data.put("from_name", fromName != null ? fromName : "DashaMail Service");

        Response response = makeRequest("POST", "mail/send", data);
        response.close();
        log.info("Email sent to: {}", to);
    }

    // Метод для получения списков подписчиков
    public List<Map<String, Object>> getLists() throws IOException {
        Response response = makeRequest("GET", "lists", null);
        String json = response.body().string();
        response.close();

        com.google.gson.reflect.TypeToken<List<Map<String, Object>>> typeToken =
                new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>() {};
        return gson.fromJson(json, typeToken.getType());
    }

    // Метод для получения подписчиков из списка
    public List<Map<String, Object>> getSubscribers(String listId) throws IOException {
        Response response = makeRequest("GET", "subscribers/list?list_id=" + listId, null);
        String json = response.body().string();
        response.close();

        com.google.gson.reflect.TypeToken<List<Map<String, Object>>> typeToken =
                new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>() {};
        return gson.fromJson(json, typeToken.getType());
    }

    // Метод для проверки статуса подписчика
    public Map<String, Object> checkSubscriber(String listId, String email) throws IOException {
        Response response = makeRequest("GET", "subscribers/check?list_id=" + listId + "&email=" + email, null);
        String json = response.body().string();
        response.close();

        com.google.gson.reflect.TypeToken<Map<String, Object>> typeToken =
                new com.google.gson.reflect.TypeToken<Map<String, Object>>() {};
        return gson.fromJson(json, typeToken.getType());
    }
}