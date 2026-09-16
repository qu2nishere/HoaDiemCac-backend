package com.hoadiemcat.service;

import com.hoadiemcat.exception.AppException;
import com.hoadiemcat.service.impl.UploadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class UploadServiceTest {

    private UploadServiceImpl uploadService;

    @BeforeEach
    void setUp() {
        // Khởi tạo mà không có Cloudinary bean để kiểm thử luồng fallback CDN nội bộ
        uploadService = new UploadServiceImpl(null);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi tệp tải lên rỗng")
    void testUploadImage_EmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
        assertThrows(AppException.class, () -> uploadService.uploadImage(emptyFile));
    }

    @Test
    @DisplayName("Ném ngoại lệ khi định dạng tệp không hợp lệ")
    void testUploadImage_InvalidFormat() {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );
        assertThrows(AppException.class, () -> uploadService.uploadImage(invalidFile));
    }

    @Test
    @DisplayName("Lưu trữ qua CDN nội bộ khi Cloudinary chưa được cấu hình")
    void testUploadImage_FallbackLocalCDN() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dish-sample.png",
                "image/png",
                "image data".getBytes()
        );

        String result = uploadService.uploadImage(file);
        assertNotNull(result);
        assertTrue(result.contains("/uploads/dishes/"));
    }
}
