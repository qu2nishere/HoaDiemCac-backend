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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

        // Ưu tiên 1: Tải lên Cloudinary CDN nếu đã cấu hình
        if (cloudinary != null) {
            try {
                Map uploadResult = cloudinary.uploader().upload(
                        file.getBytes(),
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
            } catch (Exception e) {
                log.warn("Tải ảnh lên Cloudinary gặp lỗi: {}. Chuyển sang lưu trữ CDN nội bộ.", e.getMessage());
            }
        }

        // Ưu tiên 2 (Fallback): Lưu trữ tại máy chủ Backend
        try {
            Path uploadDir = Paths.get("uploads/dishes");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String uniqueFilename = "dish-" + UUID.randomUUID() + "." + extension;
            Path destinationFile = uploadDir.resolve(uniqueFilename);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            String fileUrl = "/uploads/dishes/" + uniqueFilename;
            try {
                fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/uploads/dishes/")
                        .path(uniqueFilename)
                        .toUriString();
            } catch (Exception e) {
                log.debug("Không thể lấy context path hiện tại, sử dụng đường dẫn: {}", fileUrl);
            }

            log.info("Lưu trữ ảnh qua CDN Backend thành công: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("Lỗi khi lưu trữ tệp ảnh", e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không thể lưu tệp ảnh lên máy chủ: " + e.getMessage());
        }
    }
}
