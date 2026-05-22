package com.voxnovel.media_tts_service.service.thirdparty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.util.List;

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

    /**
     * Hàm dùng cho Tác vụ 2: Upload file vật lý (Tránh OOM cho file lớn)
     */
    public String uploadFile(String objectKey, File file) {
        log.info("Đang upload file Local lên MinIO: {}", objectKey);
        try {
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType("audio/mpeg")
                    .build();

            // S3Client sẽ stream trực tiếp từ File vật lý lên MinIO, cực kỳ tiết kiệm RAM
            s3Client.putObject(putOb, RequestBody.fromFile(file));

            String fileUrl = String.format("%s/%s/%s", minioEndpoint, bucketName, objectKey);
            log.info("✅ Upload file thành công: {}", fileUrl);

            return fileUrl;

        } catch (Exception e) {
            log.error("❌ Lỗi upload file MinIO - Key: {}", objectKey, e);
            throw new RuntimeException("Upload file lên Storage thất bại", e);
        }
    }

    /**
     * Tải file từ MinIO về máy Local (dùng cho việc kéo file nháp)
     */
    public void downloadFile(String objectKey, File destinationFile) {
        log.debug("Đang tải file từ MinIO: {}", objectKey);
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            // ResponseTransformer.toFile() giúp stream data thẳng vào file ở Local
            s3Client.getObject(getObjectRequest, ResponseTransformer.toFile(destinationFile));
        } catch (Exception e) {
            log.error("❌ Lỗi tải file từ MinIO - Key: {}", objectKey, e);
            throw new RuntimeException("Tải file từ Storage thất bại", e);
        }
    }

    /**
     * Lấy byte[] từ MinIO (dùng cho stream audio)
     */
    public byte[] getFileBytes(String objectKey) {
        log.debug("Đang lấy bytes từ MinIO: {}", objectKey);
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
        } catch (Exception e) {
            log.error("❌ Lỗi lấy bytes từ MinIO - Key: {}", objectKey, e);
            throw new RuntimeException("Lấy file từ Storage thất bại", e);
        }
    }

    /**
     * Lấy danh sách đường dẫn (Key) của tất cả các file trong một thư mục
     */
    public List<String> listFilesInDirectory(String prefix) {
        log.debug("Quét danh sách file trong thư mục: {}", prefix);
        try {
            ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response listRes = s3Client.listObjectsV2(listReq);

            // Trích xuất danh sách tên file (objectKey)
            return listRes.contents().stream()
                    .map(S3Object::key)
                    .sorted()
                    .toList();

        } catch (Exception e) {
            log.error("❌ Lỗi lấy danh sách file MinIO - Prefix: {}", prefix, e);
            throw new RuntimeException("Lấy danh sách file thất bại", e);
        }
    }
}