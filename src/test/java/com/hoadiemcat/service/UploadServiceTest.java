package com.hoadiemcat.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.hoadiemcat.exception.AppException;
import com.hoadiemcat.service.impl.UploadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

class UploadServiceTest {

    private Cloudinary cloudinary;
    private Uploader uploader;
    private UploadServiceImpl uploadService;

    @BeforeEach
    void setUp() {
        cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        uploadService = new UploadServiceImpl(cloudinary);
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

    private static final byte[] VALID_PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0
    };

    @Test
    @DisplayName("Ném ngoại lệ khi Cloudinary chưa được cấu hình (null)")
    void testUploadImage_CloudinaryNotConfigured() {
        UploadServiceImpl noCdnService = new UploadServiceImpl(null);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dish.png",
                "image/png",
                VALID_PNG_BYTES
        );
        AppException exception = assertThrows(AppException.class, () -> noCdnService.uploadImage(file));
        assertTrue(exception.getMessage().contains("Dịch vụ lưu trữ ảnh chưa sẵn sàng"));
    }

    @Test
    @DisplayName("Tải ảnh lên Cloudinary CDN thành công trả về secure_url")
    void testUploadImage_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dish-sample.png",
                "image/png",
                VALID_PNG_BYTES
        );

        String expectedUrl = "https://res.cloudinary.com/hoadiemcac/image/upload/v12345/dishes/sample.png";
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of("secure_url", expectedUrl));

        String result = uploadService.uploadImage(file);
        assertNotNull(result);
        assertEquals(expectedUrl, result);
        verify(uploader, times(1)).upload(any(byte[].class), anyMap());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi tệp là mã nhị phân/thực thi (.exe) giả mạo đuôi ảnh .jpg")
    void testUploadImage_SpoofedExeFile() {
        byte[] exeBytes = new byte[]{0x4D, 0x5A, 0x00, 0x00, 0x54, 0x68, 0x69, 0x73}; // Header MZ của file Windows PE/EXE
        MockMultipartFile spoofedFile = new MockMultipartFile(
                "file",
                "tra_sua_hongkong.jpg",
                "image/jpeg",
                exeBytes
        );
        AppException exception = assertThrows(AppException.class, () -> uploadService.uploadImage(spoofedFile));
        assertEquals(com.hoadiemcat.exception.ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("không phải là hình ảnh hợp lệ"));
    }

    @Test
    @DisplayName("Ném thông báo thân thiện khi Cloudinary từ chối nhận file do lỗi cấu trúc ảnh")
    void testUploadImage_CloudinaryInvalidImageFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dish-sample.png",
                "image/png",
                VALID_PNG_BYTES
        );

        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new RuntimeException("Invalid image file"));

        AppException exception = assertThrows(AppException.class, () -> uploadService.uploadImage(file));
        assertEquals(com.hoadiemcat.exception.ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Dịch vụ lưu trữ từ chối tệp"));
    }

    @Test
    @DisplayName("Ném ngoại lệ khi Cloudinary gặp sự cố kết nối máy chủ")
    void testUploadImage_CloudinaryError() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dish-sample.png",
                "image/png",
                VALID_PNG_BYTES
        );

        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new RuntimeException("Connection timeout to Cloudinary"));

        AppException exception = assertThrows(AppException.class, () -> uploadService.uploadImage(file));
        assertTrue(exception.getMessage().contains("Không thể tải ảnh lên dịch vụ lưu trữ"));
    }
}
