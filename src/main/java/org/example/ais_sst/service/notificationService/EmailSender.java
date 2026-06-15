//package org.example.ais_sst.service.notificationService;
//
//import com.google.gson.Gson;
//import okhttp3.MediaType;
//import okhttp3.RequestBody;
//import okhttp3.Response;
//
//import java.util.HashMap;
//import java.util.Map;
//
//public class EmailSender {
//    private final DashaMailClient client;
//
//    public EmailSender(DashaMailClient client) {
//        this.client = client;
//    }
//
//    public void sendEmail(String to, String subject,
//                          String htmlContent) throws Exception {
//        Map<String, Object> data = new HashMap<>();
//        data.put("to", to);
//        data.put("subject", subject);
//        data.put("html", htmlContent);
//        data.put("from_name", "Ваш сервис");
//        data.put("from_email", "noreply@yourdomain.com");
//
//        String json = new Gson().toJson(data);
//        RequestBody body = RequestBody.create(
//                json, MediaType.parse("application/json")
//        );
//
//        Response response = client.makeRequest("POST",
//                "mail/send", body);
//
//        if (response.isSuccessful()) {
//            System.out.println("Письмо отправлено");
//        }
//    }
//}