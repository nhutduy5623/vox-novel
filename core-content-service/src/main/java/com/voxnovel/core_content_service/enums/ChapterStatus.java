package com.voxnovel.core_content_service.enums;

public enum ChapterStatus {
    DRAFT,          // Bản nháp, tác giả đang viết
    PROCESSING_AI,  // Đang chờ AI tạo kịch bản/audio
    REVIEWING,      // Admin đang duyệt/chỉnh sửa
    PUBLISHED,       // Đã xuất bản, người dùng có thể đọc/nghe
    FAILED
}
