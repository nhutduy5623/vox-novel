# Vox-Novel 🎙️📚
**Nền tảng Sách nói Phân vai Tự động bằng Trí tuệ Nhân tạo**

## 1. Giới thiệu
**Vox-Novel** là một hệ thống chuyển thể truyện chữ (tiểu thuyết) thành sách nói (audiobook) sinh động. Khác với các hệ thống Text-to-Speech (TTS) thông thường chỉ đọc đều đều một giọng, Vox-Novel sử dụng **AI (Large Language Models)** để tự động bóc tách kịch bản, xác định xem nhân vật nào đang nói câu nào, đoạn nào là lời của người dẫn chuyện. Sau đó, hệ thống gọi các dịch vụ TTS bên thứ ba để lồng tiếng cho từng nhân vật với các chất giọng khác nhau, cuối cùng tự động chuẩn hóa và ghép nối thành một file audio chương truyện hoàn chỉnh.

## 2. Công nghệ & Kiến trúc Hệ thống

Dự án được thiết kế theo kiến trúc **Microservices** để đảm bảo tính mở rộng và chịu tải cao.

### 🛠️ Technology Stack
- **Backend:** Java 17+, Spring Boot 3, Spring Cloud, Spring AI (tích hợp Gemini).
- **Frontend:** TypeScript, Angular.
- **Database:** PostgreSQL (Lưu trữ dữ liệu cốt lõi).
- **Message Broker:** RabbitMQ (Giao tiếp bất đồng bộ giữa các services).
- **Cache & Distributed Lock:** Redis (Quản lý xoay vòng API Key và giới hạn rate-limit).
- **Object Storage:** MinIO (Giả lập AWS S3 để lưu trữ file âm thanh mp3 nháp và hoàn chỉnh).
- **Audio Processing:** FFmpeg (Chạy ngầm ở local để chuẩn hóa âm lượng `loudnorm` và ghép nối file siêu tốc).
- **Containerization:** Docker & Docker Compose.

### 🧩 Các Microservices
1. **`api-gateway`**: Cổng vào duy nhất, định tuyến (Routing) và xác thực (Authentication).
2. **`identity-service`**: Quản lý tài khoản, phân quyền, thanh toán, gói VIP và lịch sử đọc/nghe của người dùng.
3. **`core-content-service`**: Quản lý lõi dữ liệu truyện (Novel, Chapter, Character, Voice).
4. **`ai-engine-service`**: Dịch vụ xử lý AI. Gọi LLM (Gemini 3.5 Flash) để phân tách kịch bản phân vai.
5. **`media-tts-service`**: Dịch vụ xử lý âm thanh. Gọi API của FPT AI TTS, Camb.ai TTS, lưu trữ lên MinIO và gọi FFmpeg để xử lý chuẩn hóa, ghép nối âm thanh.

---

## 3. Luồng Logic Chạy (Business Flow)
Dự án giải quyết bài toán "Truyện chữ -> Sách nói phân vai" qua 5 bước tự động hóa:

1. **Nhập liệu & Thiết lập:** Admin/Tác giả đăng truyện, đăng chương mới và khai báo danh sách nhân vật (Kèm giới tính, mô tả, giọng đọc đại diện).
2. **Phân vai bằng AI (AI Script Generation):** 
   - `core-content-service` đẩy thông báo qua RabbitMQ.
   - `ai-engine-service` nhận lệnh, cắt nhỏ text chương truyện, truyền ngữ cảnh trước/sau vào prompt và gọi **Gemini API** để lấy về một kịch bản JSON (Mỗi dòng thoại gắn ID của một nhân vật hoặc Người dẫn chuyện).
3. **Duyệt kịch bản (Human in the loop):** Kịch bản trả về có thể được người dùng/Admin xem lại và chỉnh sửa thủ công trên giao diện nếu AI nhận diện sai người nói.
4. **Tạo giọng thoại đơn lẻ (TTS Generation):**
   - Lệnh được đẩy qua RabbitMQ đến `media-tts-service`.
   - Dịch vụ này map ID nhân vật với Provider TTS, gọi API để tạo file mp3 cho *từng câu thoại* và đẩy lên thư mục `drafts/` trên MinIO.
5. **Chuẩn hóa & Ghép nối (Merge Audio):**
   - `media-tts-service` tải toàn bộ mp3 nháp từ MinIO về máy chủ cục bộ.
   - Dùng lệnh ProcessBuilder gọi FFmpeg chạy song song (`parallelStream`) để chuẩn hóa âm lượng (`loudnorm`) cho mọi file, đảm bảo tiếng nhân vật và dẫn truyện đều đặn.
   - Gọi `ffmpeg concat` ghép nối các file nháp thành 1 file sách nói hoàn chỉnh trong chớp mắt.
   - Upload file cuối lên `production/` trên MinIO, cập nhật link vào DB, đổi trạng thái chương thành `PUBLISHED`.

---

## 4. Những Logic Đáng Chú Ý Đã Triển Khai
- **Quản lý API Key Pool bằng Redis:** Xây dựng cơ chế Load Balancer cho các API key (Gemini, FPT, Camb.ai) để tránh bị Rate Limit. Tự động phát hiện và cách ly (isolate) các Key bị chết/hết hạn ngạch.
- **Prompt Engineering thông minh:** Đưa `previous_context` và `next_context` vào prompt của Gemini để AI đoán đúng đại từ nhân xưng (hắn, y, nàng, lão...).
- **Xử lý FFmpeg Đa luồng:** Tận dụng CPU của server để chạy chuẩn hóa âm lượng nhiều file `.mp3` cùng lúc giúp tiết kiệm 80% thời gian tạo audio.

---

## 5. Hướng Dẫn Chạy Dự Án Từ Con Số 0 (Getting Started)

### Yêu cầu hệ thống (Prerequisites)
- **Java 17** (hoặc mới hơn) & Maven.
- **Node.js** (Phiên bản LTS) & npm.
- **Docker** & **Docker Compose**.
- **FFmpeg**: Yêu cầu cài đặt FFmpeg trên máy thật và đưa vào biến môi trường (Environment Variable `PATH`). Kiểm tra bằng lệnh: `ffmpeg -version`.
- IDE: IntelliJ IDEA (cho Backend) và VS Code (cho Frontend).

### Bước 1: Khởi chạy Hạ tầng (Infrastructure)
1. Mở file `docker-compose.yml` ở thư mục gốc của dự án. Đảm bảo bạn đã **bỏ comment** (uncomment) các services: `postgres`, `rabbitmq`, `redis`, `minio`.
2. Mở terminal tại thư mục gốc và chạy:
   ```bash
   docker compose up -d
   ```
3. Chờ các container khởi động. Bạn có thể truy cập:
   - RabbitMQ Management: `http://localhost:15672` (guest/guest)
   - MinIO Console: `http://localhost:9001` (minio_admin/minio_password_123)

### Bước 2: Chuẩn bị Biến Môi Trường & API Keys
Trong các project Spring Boot (đặc biệt là `ai-engine-service` và `media-tts-service`), bạn cần cung cấp API Keys thực tế để hệ thống hoạt động:
- **Gemini API Key:** Dành cho tính năng phân vai kịch bản. Điền vào cấu hình của `ai-engine-service` (file `.env` hoặc `application.yml`).
- **FPT AI / Camb.ai Keys:** Dành cho TTS. Điền vào cấu hình của `media-tts-service`.
- *(Lưu ý: Nếu chưa có Keys thật, hệ thống sẽ báo lỗi khi cố gọi AI hoặc TTS).*

### Bước 3: Khởi chạy Backend Services
Nên mở toàn bộ dự án bằng IntelliJ IDEA. Bật công cụ **Services (Run Dashboard)** để quản lý.
Chạy lần lượt các dịch vụ Spring Boot theo thứ tự sau (tránh lỗi khởi động):
1. **`api-gateway`** (Bắt buộc chạy trước hoặc cùng lúc để nhận diện routes).
2. **`identity-service`**
3. **`core-content-service`**
4. **`ai-engine-service`**
5. **`media-tts-service`**

*Tip: Khi chạy lần đầu, Hibernate sẽ tự động tạo bảng (DDL update) trong PostgreSQL.*

### Bước 4: Khởi chạy Frontend
Mở terminal mới và chuyển hướng vào thư mục Frontend:
```bash
cd vox-frontend
npm install
npm run start  # Hoặc: ng serve
```
Truy cập ứng dụng tại trình duyệt: `http://localhost:4200` (hoặc cổng cấu hình trong Angular).

### Bước 5: Test luồng hoạt động
1. Tạo tài khoản / Đăng nhập.
2. Thêm một truyện mới -> Thêm một nhân vật (gán giọng đọc) -> Thêm một chương mới (paste text văn bản).
3. Nhấn nút **"Phân vai AI"** trên giao diện.
4. Xem lại kịch bản JSON do AI sinh ra.
5. Nhấn nút **"Tạo Audio"**. Mở log của `media-tts-service` để xem tiến trình gọi TTS và FFmpeg.
6. Thưởng thức thành quả! 🎉
