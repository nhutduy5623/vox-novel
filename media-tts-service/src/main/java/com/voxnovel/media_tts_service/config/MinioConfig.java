package com.voxnovel.media_tts_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class MinioConfig {

    @Value("${voxnovel.minio.endpoint}")
    private String endpoint; // http://localhost:9000

    @Value("${voxnovel.minio.access-key}")
    private String accessKey; // root_admin

    @Value("${voxnovel.minio.secret-key}")
    private String secretKey; // root_password_123

    @Bean
    public S3Client s3Client() {
        // Đây là nơi ta lắp ráp "chiếc điện thoại vệ tinh"
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint)) // Trỏ về MinIO thay vì trỏ lên AWS Cloud
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey))) // Đưa chìa khóa vào
                .region(Region.US_EAST_1) // MinIO bắt buộc phải có Region ảo, khai báo đại US_EAST_1
                .forcePathStyle(true)     // Cờ BẮT BUỘC bật lên khi gọi API giả lập S3 như MinIO
                .build();
    }
}