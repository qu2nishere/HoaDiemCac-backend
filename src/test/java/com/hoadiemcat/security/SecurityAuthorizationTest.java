package com.hoadiemcat.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoadiemcat.dto.request.MenuItemRequest;
import com.hoadiemcat.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // =========================================================================
    // UC41 - TẢI ẢNH LÊN CLOUDINARY CDN (POST /api/v1/public/upload/image)
    // =========================================================================

    @Test
    @DisplayName("UC41: Gửi request upload ảnh khi chưa đăng nhập (không token) -> Bị từ chối HTTP 401 Unauthorized")
    void testUploadImage_Unauthenticated_Returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "dummy image content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/public/upload/image").file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
    }

    @Test
    @WithMockUser(username = "staff_waiter", roles = {"WAITER"})
    @DisplayName("UC41: Nhân viên phục vụ (role WAITER) upload ảnh -> Bị từ chối HTTP 403 Forbidden")
    void testUploadImage_WaiterRole_Returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "dummy image content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/public/upload/image").file(file))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @WithMockUser(username = "chef_kitchen", roles = {"KITCHEN"})
    @DisplayName("UC41: Nhân viên bếp (role KITCHEN) upload ảnh -> Bị từ chối HTTP 403 Forbidden")
    void testUploadImage_KitchenRole_Returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "dummy image content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/public/upload/image").file(file))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @WithMockUser(username = "manager_user", roles = {"MANAGER"})
    @DisplayName("UC41: Quản lý (role MANAGER) upload ảnh -> Cho phép truy cập (Không bị 401/403)")
    void testUploadImage_ManagerRole_Allowed() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46}
        );

        // Request được phép qua tầng bảo mật (sẽ được xử lý bởi UploadController/UploadService)
        mockMvc.perform(multipart("/api/v1/public/upload/image").file(file))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertNotEquals(401, status, "Không được trả về 401 khi đã có role MANAGER");
                    org.junit.jupiter.api.Assertions.assertNotEquals(403, status, "Không được trả về 403 khi đã có role MANAGER");
                });
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("UC41: Quản trị viên (role ADMIN) upload ảnh -> Cho phép truy cập (Không bị 401/403)")
    void testUploadImage_AdminRole_Allowed() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46}
        );

        mockMvc.perform(multipart("/api/v1/public/upload/image").file(file))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertNotEquals(401, status, "Không được trả về 401 khi đã có role ADMIN");
                    org.junit.jupiter.api.Assertions.assertNotEquals(403, status, "Không được trả về 403 khi đã có role ADMIN");
                });
    }

    // =========================================================================
    // THÊM MÓN ĂN MỚI (POST /api/v1/menu-items)
    // =========================================================================

    @Test
    @DisplayName("Thêm món ăn khi chưa đăng nhập (không token) -> Bị từ chối HTTP 401 Unauthorized")
    void testCreateMenuItem_Unauthenticated_Returns401() throws Exception {
        MenuItemRequest request = MenuItemRequest.builder()
                .name("Món Ăn Test Bảo Mật")
                .categoryId("nuoc-lau")
                .price(new BigDecimal("150000"))
                .unit("Phần")
                .build();

        mockMvc.perform(post("/api/v1/menu-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
    }

    @Test
    @WithMockUser(username = "staff_waiter", roles = {"WAITER"})
    @DisplayName("Nhân viên phục vụ (role WAITER) thêm món ăn -> Bị từ chối HTTP 403 Forbidden")
    void testCreateMenuItem_WaiterRole_Returns403() throws Exception {
        MenuItemRequest request = MenuItemRequest.builder()
                .name("Món Ăn Test Bảo Mật")
                .categoryId("nuoc-lau")
                .price(new BigDecimal("150000"))
                .unit("Phần")
                .build();

        mockMvc.perform(post("/api/v1/menu-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @WithMockUser(username = "cashier_user", roles = {"CASHIER"})
    @DisplayName("Thu ngân (role CASHIER) thêm món ăn -> Bị từ chối HTTP 403 Forbidden")
    void testCreateMenuItem_CashierRole_Returns403() throws Exception {
        MenuItemRequest request = MenuItemRequest.builder()
                .name("Món Ăn Test Bảo Mật")
                .categoryId("nuoc-lau")
                .price(new BigDecimal("150000"))
                .unit("Phần")
                .build();

        mockMvc.perform(post("/api/v1/menu-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @WithMockUser(username = "manager_user", roles = {"MANAGER"})
    @DisplayName("Quản lý (role MANAGER) thêm món ăn -> Cho phép truy cập (Không bị 401/403)")
    void testCreateMenuItem_ManagerRole_Allowed() throws Exception {
        MenuItemRequest request = MenuItemRequest.builder()
                .name("Bò Wagyu Hỏa Diệm")
                .categoryId("nuoc-lau")
                .price(new BigDecimal("250000"))
                .unit("Đĩa")
                .isAvailable(true)
                .build();

        mockMvc.perform(post("/api/v1/menu-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertNotEquals(401, status, "Không được trả về 401 khi đã có role MANAGER");
                    org.junit.jupiter.api.Assertions.assertNotEquals(403, status, "Không được trả về 403 khi đã có role MANAGER");
                });
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("Quản trị viên (role ADMIN) thêm món ăn -> Cho phép truy cập (Không bị 401/403)")
    void testCreateMenuItem_AdminRole_Allowed() throws Exception {
        MenuItemRequest request = MenuItemRequest.builder()
                .name("Bò Wagyu Hỏa Diệm Admin")
                .categoryId("nuoc-lau")
                .price(new BigDecimal("290000"))
                .unit("Đĩa")
                .isAvailable(true)
                .build();

        mockMvc.perform(post("/api/v1/menu-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertNotEquals(401, status, "Không được trả về 401 khi đã có role ADMIN");
                    org.junit.jupiter.api.Assertions.assertNotEquals(403, status, "Không được trả về 403 khi đã có role ADMIN");
                });
    }

    // =========================================================================
    // ENDPOINTS CÔNG KHAI (GET)
    // =========================================================================

    @Test
    @DisplayName("Khách hàng truy cập danh sách thực đơn (GET /api/v1/menu-items) không cần đăng nhập -> 200 OK")
    void testGetMenuItems_Public_Success() throws Exception {
        mockMvc.perform(get("/api/v1/menu-items"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Truy cập Health Check (GET /api/v1/public/health) không cần đăng nhập -> 200 OK")
    void testHealthCheck_Public_Success() throws Exception {
        mockMvc.perform(get("/api/v1/public/health"))
                .andExpect(status().isOk());
    }
}
