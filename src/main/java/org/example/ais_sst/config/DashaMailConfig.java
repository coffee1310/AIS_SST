//package org.example.ais_sst.config;
//
//import com.example.dashamail.client.DashaMailClient;
//import lombok.extern.slf4j.Slf4j;
//import okhttp3.OkHttpClient;
//import okhttp3.logging.HttpLoggingInterceptor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.concurrent.TimeUnit;
//
//@Slf4j
//@Configuration
//public class DashaMailConfig {
//
//    @Value("${dashamail.api.key}")
//    private String apiKey;
//
//    @Value("${dashamail.api.url:https://api.dashamail.com/}")
//    private String apiUrl;
//
//    @Value("${dashamail.timeout:30}")
//    private int timeout;
//
//    @Bean
//    public OkHttpClient okHttpClient() {
//        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(log::debug);
//        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
//
//        return new OkHttpClient.Builder()
//                .connectTimeout(timeout, TimeUnit.SECONDS)
//                .readTimeout(timeout, TimeUnit.SECONDS)
//                .writeTimeout(timeout, TimeUnit.SECONDS)
//                .addInterceptor(loggingInterceptor)
//                .build();
//    }
//
//    @Bean
//    public DashaMailClient dashaMailClient(OkHttpClient okHttpClient) {
//        return new DashaMailClient(apiKey, apiUrl, okHttpClient);
//    }
//
//    @Bean
//    public RestTemplate restTemplate() {
//        return new RestTemplate();
//    }
//}