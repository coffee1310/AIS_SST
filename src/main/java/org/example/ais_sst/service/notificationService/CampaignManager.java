//package org.example.ais_sst.service.notificationService;
//
//import java.util.List;
//import java.util.ArrayList;
//import java.lang.reflect.Type;
//import com.google.gson.Gson;
//import com.google.gson.reflect.TypeToken;
//import okhttp3.Response;
//
//
//public class CampaignManager {
//    private final DashaMailClient client;
//
//    public CampaignManager(DashaMailClient client) {
//        this.client = client;
//    }
//
//    public List<Campaign> getCampaigns() throws Exception {
//        Response response = client.makeRequest("GET",
//                "campaigns/list", null);
//
//        if (response.isSuccessful()) {
//            String json = response.body().string();
//            Type type = new TypeToken<List<Campaign>>(){}.getType();
//            return new Gson().fromJson(json, type);
//        }
//        return new ArrayList<>();
//    }
//}