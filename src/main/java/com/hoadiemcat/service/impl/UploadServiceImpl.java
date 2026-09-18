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

        if (cloudinary == null) {
            log.error("Cloudinary chưa được cấu hình thông tin kết nối (cloud_name, api_key, api_secret)");
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION,
                    "Dịch vụ Cloudinary CDN chưa được cấu hình. Vui lòng cấu hình CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET để tải ảnh lên CDN.");
        }

        try {
            // Tải ảnh trực tiếp lên Cloudinary CDN từ bộ nhớ RAM (không lưu bất kỳ tệp nào vào máy tính)
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

            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không nhận được đường dẫn ảnh an toàn từ Cloudinary CDN");

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi tải ảnh lên Cloudinary CDN: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không thể tải ảnh lên Cloudinary CDN: " + e.getMessage());
        }
    }
}
