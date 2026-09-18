package com.hoadiemcat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cấu hình Web MVC cho dự án Hỏa Diệm Các.
 * Toàn bộ hình ảnh món ăn được lưu trữ và phân phối trực tiếp qua Cloudinary CDN,
 * không sử dụng lưu trữ tệp cục bộ trên ổ đĩa máy tính.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    // Không phục vụ tệp tĩnh từ ổ đĩa cục bộ
}
