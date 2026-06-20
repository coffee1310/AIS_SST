package org.example.ais_sst.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

import java.net.URI;

@Configuration
public class PostboxConfig {

    @Value("${yandex.postbox.access-key}")
    private String accessKey;

    @Value("${yandex.postbox.secret-key}")
    private String secretKey;

    @Value("${yandex.postbox.endpoint}")
    private String endpoint;

    @Bean
    public SesV2Client sesV2Client() {
        return SesV2Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .endpointOverride(URI.create("https://postbox.cloud.yandex.net"))
                .region(Region.of("ru-central1"))   // без дефиса!
                .build();
    }
}