package com.hoadiemcat.controller.api;

import com.hoadiemcat.dto.response.ApiResponse;
import com.hoadiemcat.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/public/upload")
@RequiredArgsConstructor
@Tag(name = "Upload CDN", description = "APIs tải lên hình ảnh món ăn lưu trữ CDN")
@Slf4j
public class UploadController {

    private final UploadService uploadService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải ảnh món ăn lên CDN", description = "Nhận file ảnh từ máy tính, tải lên Cloudinary CDN (hoặc CDN nội bộ) và trả về link URL ảnh trực tiếp")
    public ResponseEntity<ApiResponse<String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String fileUrl = uploadService.uploadImage(file);
        return ResponseEntity.ok(ApiResponse.success("Tải ảnh lên CDN thành công", fileUrl));
    }
}
