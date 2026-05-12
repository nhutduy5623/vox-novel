package com.voxnovel.media_tts_service.service.thirdparty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final S3Client s3Client;

    @Value("${voxnovel.minio.bucket-name}")
    private String bucketName;

    @Value("${voxnovel.minio.endpoint}")
    private String minioEndpoint;

    /**
     * Hàm dùng cho Tác vụ 1: Nhận byte audio từ TTS và đẩy lên kho
     * Trả về URL để lưu vào Database.
     */
    public String uploadAudioBytes(String objectKey, byte[] audioData) {
        log.info("Đang tiến hành đẩy file lên Storage với Key: {}", objectKey);
        try {
            // Xây dựng thông tin gói hàng (Chỉ định bucket, tên file, và định dạng là mp3)
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType("audio/mpeg")
                    .build();

            // Tiến hành upload luồng byte
            s3Client.putObject(putOb, RequestBody.fromBytes(audioData));

            // Xây dựng đường dẫn URL hoàn chỉnh trả về cho Client
            String fileUrl = String.format("%s/%s/%s", minioEndpoint, bucketName, objectKey);
            log.info("✅ Upload thành công: {}", fileUrl);

            return fileUrl;

        } catch (Exception e) {
            log.error("❌ Lỗi upload MinIO - Key: {}", objectKey, e);
            throw new RuntimeException("Upload file lên Storage thất bại", e);
        }
    }

    // TODO: Sau này làm Tác vụ 2 (Ghép file FFmpeg), chúng ta sẽ viết thêm hàm downloadFile() tại đây
}