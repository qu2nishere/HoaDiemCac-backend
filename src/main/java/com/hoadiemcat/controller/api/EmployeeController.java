package com.hoadiemcat.controller.api;

import com.hoadiemcat.dto.request.EmployeeCreateRequest;
import com.hoadiemcat.dto.response.ApiResponse;
import com.hoadiemcat.entity.User;
import com.hoadiemcat.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> createEmployee(@Valid @RequestBody EmployeeCreateRequest request) {
        User user = employeeService.createEmployee(request);
        
        return ResponseEntity.ok(ApiResponse.success("Tạo tài khoản nhân viên thành công", user.getId()));
    }

    @org.springframework.web.bind.annotation.GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<com.hoadiemcat.dto.response.EmployeeResponse>>> getAllEmployees() {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getAllEmployees()));
    }
}
