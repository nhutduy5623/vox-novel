package com.voxnovel.media_tts_service.service;

import com.voxnovel.media_tts_service.dto.request.AudioMergeRequest;
import com.voxnovel.media_tts_service.service.thirdparty.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioMergeService {
    private final MinioService minioService;

    private static final String FFMPEG_WORK_DIR = System.getProperty("java.io.tmpdir") + File.separator + "ffmpeg_work" + File.separator;

    public String processMerge(AudioMergeRequest request) throws Exception {
        String novelId = request.novelId();
        String chapterId = request.chapterId();

        // 1. Quét MinIO
        String draftPrefix = String.format("drafts/%s/%s/", novelId, chapterId);
        List<String> minioFiles = minioService.listFilesInDirectory(draftPrefix);

        if (minioFiles.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy file nháp nào trong MinIO cho chapter: " + chapterId);
        }

        Path workDirPath = Path.of(FFMPEG_WORK_DIR, chapterId);
        Path finalAudioLocalPath = Path.of(FFMPEG_WORK_DIR, chapterId + "_final.mp3");
        String finalMinioPath = String.format("production/%s/%s.mp3", novelId, chapterId);

        try {
            Files.createDirectories(workDirPath);

            // 3. Tải file từ MinIO về Local
            downloadDraftFilesToLocal(minioFiles, workDirPath);

            // Bước 4: CHUẨN HÓA ĐỒNG LOẠT (Chạy đa luồng song song)
            List<String> normalizedFiles = normalizeAudioFiles(minioFiles, workDirPath);

            // 5. Chạy FFmpeg concat
            runFfmpegConcat(normalizedFiles, workDirPath, finalAudioLocalPath);

            // 6. Upload file final lên MinIO
            log.info("Uploading final audio to MinIO: {}", finalMinioPath);
            String audioUrl = minioService.uploadFile(finalMinioPath, finalAudioLocalPath.toFile());

            return audioUrl;

        } finally {
            // 6. Clean up
            cleanUpLocalWorkspace(workDirPath, finalAudioLocalPath);
        }
    }

    private void downloadDraftFilesToLocal(List<String> minioFiles, Path workDirPath) {
        log.info("Bắt đầu tải {} files về {}", minioFiles.size(), workDirPath);

        minioFiles.parallelStream().forEach(objectName -> {
            try {
                // Trích xuất tên file từ objectName (VD: drafts/novelA/chap1/001_narrator.mp3 -> 001_narrator.mp3)
                String fileName = Path.of(objectName).getFileName().toString();
                Path localFilePath = workDirPath.resolve(fileName);

                // Giả định MinioService có hàm downloadFile(String minioPath, File localFile)
                minioService.downloadFile(objectName, localFilePath.toFile());
            } catch (Exception e) {
                log.error("Lỗi khi tải file từ MinIO: {}", objectName, e);
                throw new RuntimeException("Download failed for " + objectName, e);
            }
        });
    }

    // --- HÀM MỚI: Chuẩn hóa từng file song song ---
    private List<String> normalizeAudioFiles(List<String> minioFiles, Path workDirPath) {
        log.info("Bắt đầu chuẩn hóa (Normalize) {} files...", minioFiles.size());

        // Dùng parallelStream để tận dụng tối đa CPU server, chuẩn hóa cùng lúc nhiều file
        return minioFiles.parallelStream().map(objectName -> {
            String originalFileName = Path.of(objectName).getFileName().toString();
            String normFileName = originalFileName.replace(".mp3", "_norm.mp3");

            Path inputPath = workDirPath.resolve(originalFileName);
            Path outputPath = workDirPath.resolve(normFileName);

            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "ffmpeg", // Hoặc đường dẫn tuyệt đối C:\\ffmpeg\\bin\\ffmpeg.exe
                        "-y",     // Tự động ghi đè nếu file tồn tại
                        "-i", inputPath.toAbsolutePath().toString(),

                        // Ép Format
                        "-ar", "44100",
                        "-ac", "2",
                        "-b:a", "128k",

                        // Ép Âm lượng
                        "-af", "loudnorm=I=-16:TP=-1.5:LRA=11",

                        outputPath.toAbsolutePath().toString()
                );

                Process process = pb.start();
                int exitCode = process.waitFor(); // Chờ file này xử lý xong

                if (exitCode != 0) {
                    throw new RuntimeException("Lỗi FFmpeg khi chuẩn hóa file: " + originalFileName);
                }
                return normFileName; // Trả về tên file đã được chuẩn hóa

            } catch (Exception e) {
                log.error("Lỗi xử lý file {}: {}", originalFileName, e.getMessage());
                throw new RuntimeException(e);
            }
        }).toList();
    }

    // --- SỬA LẠI HÀM CŨ: Ghép nối file siêu tốc ---
    private void runFfmpegConcat(List<String> filesToMerge, Path workDirPath, Path finalAudioLocalPath) throws Exception {
        log.info("Chuẩn bị FFmpeg concat tại {}", workDirPath);

        Path inputsTxtPath = workDirPath.resolve("inputs.txt");
        List<String> ffmpegInputLines = filesToMerge.stream()
                .map(fileName -> "file '" + fileName + "'")
                .toList();

        Files.write(inputsTxtPath, ffmpegInputLines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-y",  // BẮT BUỘC CÓ
                "-f", "concat",
                "-safe", "0",
                "-i", "inputs.txt",
                "-c", "copy",
                finalAudioLocalPath.toAbsolutePath().toString()
        );
        pb.directory(workDirPath.toFile());

        // Gộp Error và Output Stream để xả log
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // BẮT BUỘC PHẢI CÓ ĐOẠN NÀY ĐỂ XẢ BUFFER TRÁNH TREO
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Đọc log của FFmpeg vứt đi cho trống bộ đệm
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg concat process exited with error code: " + exitCode);
        }

        log.info("FFmpeg concat hoàn tất: {}", finalAudioLocalPath);
    }

    private void cleanUpLocalWorkspace(Path workDirPath, Path finalAudioLocalPath) {
        log.info("Dọn dẹp thư mục tạm: {}", workDirPath);
        try {
            // Xóa file final nếu có (nó nằm ngoài workDirPath)
            Files.deleteIfExists(finalAudioLocalPath);

            // Xóa thư mục làm việc (đệ quy xóa file bên trong)
            if (Files.exists(workDirPath)) {
                try (Stream<Path> walk = Files.walk(workDirPath)) {
                    walk.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi dọn dẹp workspace: {}", e.getMessage());
        }
    }
}
