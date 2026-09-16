package com.hoadiemcat.service;

import com.hoadiemcat.dto.request.EmployeeCreateRequest;
import com.hoadiemcat.dto.response.EmployeeResponse;
import com.hoadiemcat.entity.User;

import java.util.List;

public interface EmployeeService {
    User createEmployee(EmployeeCreateRequest request);
    List<EmployeeResponse> getAllEmployees();
}
