package com.hoadiemcat.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.hoadiemcat.exception.AppException;
import com.hoadiemcat.exception.ErrorCode;
import com.hoadiemcat.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class UploadServiceImpl implements UploadService {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final Cloudinary cloudinary;

    public UploadServiceImpl(@Autowired(required = false) Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Tệp tải lên không được để trống");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Dung lượng ảnh tối đa là 10MB");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg");
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex + 1).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Định dạng tệp không được hỗ trợ. Chỉ chấp nhận jpg, jpeg, png, webp, gif");
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Không thể đọc nội dung tệp hình ảnh");
        }

        // Kiểm tra cấu trúc byte thực tế (Magic Bytes) để phát hiện tệp không phải hình ảnh hợp lệ
        if (!isValidImageMagicBytes(fileBytes)) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Tệp tải lên không phải là hình ảnh hợp lệ. Vui lòng chọn lại tệp ảnh thực tế (JPG, PNG, WEBP, GIF).");
        }

        if (cloudinary == null) {
            log.error("Cloudinary chưa được cấu hình thông tin kết nối (cloud_name, api_key, api_secret)");
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION,
                    "Dịch vụ lưu trữ ảnh chưa sẵn sàng. Vui lòng liên hệ quản trị viên hệ thống.");
        }

        try {
            // Tải ảnh trực tiếp lên Cloudinary CDN từ bộ nhớ RAM (không lưu bất kỳ tệp nào vào máy tính)
            Map uploadResult = cloudinary.uploader().upload(
                    fileBytes,
                    ObjectUtils.asMap(
                            "folder", "hoadiemcac/dishes",
                            "resource_type", "image"
                    )
            );

            String secureUrl = (String) uploadResult.get("secure_url");
            if (secureUrl != null && !secureUrl.isBlank()) {
                log.info("Tải ảnh lên Cloudinary CDN thành công: {}", secureUrl);
                return secureUrl;
            }

            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không nhận được đường dẫn ảnh an toàn từ dịch vụ lưu trữ");

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi tải ảnh lên Cloudinary CDN: {}", e.getMessage(), e);
            String rawMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (rawMsg.contains("invalid image file") || rawMsg.contains("format not supported") || rawMsg.contains("corrupt")) {
                throw new AppException(ErrorCode.INVALID_REQUEST,
                        "Dịch vụ lưu trữ từ chối tệp: Định dạng ảnh không hợp lệ hoặc tệp ảnh bị hỏng. Vui lòng chọn tệp ảnh khác.");
            }
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION,
                    "Không thể tải ảnh lên dịch vụ lưu trữ đám mây. Vui lòng thử lại sau.");
        }
    }

    /**
     * Xác thực cấu trúc Magic Bytes đầu tệp nhằm phát hiện tệp thực thi hoặc giả mạo đuôi mở rộng
     */
    private boolean isValidImageMagicBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }

        // Phát hiện tệp thực thi Windows PE (.exe, .dll, .sys): 0x4D, 0x5A ("MZ")
        if (bytes[0] == 0x4D && bytes[1] == 0x5A) {
            return false;
        }

        // Phát hiện tệp thực thi Linux ELF: 0x7F, 'E', 'L', 'F'
        if (bytes[0] == 0x7F && bytes[1] == 0x45 && bytes[2] == 0x4C && bytes[3] == 0x46) {
            return false;
        }

        // Phát hiện tệp nén / Office / Java Archive (PK..): 0x50, 0x4B, 0x03, 0x04
        if (bytes[0] == 0x50 && bytes[1] == 0x4B && bytes[2] == 0x03 && bytes[3] == 0x04) {
            return false;
        }

        // Phát hiện tệp tài liệu PDF (%PDF): 0x25, 0x50, 0x44, 0x46
        if (bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46) {
            return false;
        }

        // 1. JPEG: FF D8 FF
        boolean isJpeg = (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
        if (isJpeg) return true;

        // 2. PNG: 89 50 4E 47 0D 0A 1A 0A
        if (bytes.length >= 8) {
            boolean isPng = (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47 &&
                    bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A;
            if (isPng) return true;
        }

        // 3. GIF: GIF87a / GIF89a -> 47 49 46 38
        boolean isGif = bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8';
        if (isGif) return true;

        // 4. WEBP: RIFF....WEBP -> 52 49 46 46 .... 57 45 42 50
        if (bytes.length >= 12) {
            boolean isWebp = bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' &&
                    bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            if (isWebp) return true;
        }

        return false;
    }
}
