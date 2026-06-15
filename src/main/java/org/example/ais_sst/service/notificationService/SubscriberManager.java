//package org.example.ais_sst.service.notificationService;
//
//import com.google.gson.Gson;
//import okhttp3.MediaType;
//import okhttp3.RequestBody;
//import okhttp3.Response;
//
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//
//public class SubscriberManager {
//    private final DashaMailClient client;
//
//    public SubscriberManager(DashaMailClient client) {
//        this.client = client;
//    }
//
//    public void addSubscriber(String listId, String email,
//                              String name) throws Exception {
//        Map<String, Object> data;
//        data = new HashMap<>();
//        data.put("list_id", listId);
//        data.put("email", email);
//        data.put("name", name);
//
//        String json = new Gson().toJson(data);
//        RequestBody body = RequestBody.create(
//                json, MediaType.parse("application/json")
//        );
//
//        Response response = client.makeRequest("POST",
//                "subscribers/add", body);
//
//        if (response.isSuccessful()) {
//            System.out.println("Подписчик добавлен");
//        } else {
//            throw new IOException("Ошибка: " + response.code());
//        }
//    }
//}