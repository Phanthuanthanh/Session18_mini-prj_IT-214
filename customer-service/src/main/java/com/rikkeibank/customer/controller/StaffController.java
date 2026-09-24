package com.rikkeibank.customer.controller;

import com.rikkeibank.common.dto.ApiResponse;
import com.rikkeibank.customer.entity.Staff;
import com.rikkeibank.customer.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staffs")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Staff>>> getAllStaffs() {
        return ResponseEntity.ok(ApiResponse.success(staffService.getAllStaffs()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Staff>> getStaffById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(staffService.getStaffById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Staff>> createStaff(@Valid @RequestBody Staff staff) {
        Staff created = staffService.createStaff(staff);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo mới nhân viên/giao dịch viên thành công", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Staff>> updateStaff(
            @PathVariable Long id,
            @Valid @RequestBody Staff staff) {
        Staff updated = staffService.updateStaff(id, staff);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin nhân viên thành công", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStaff(@PathVariable Long id) {
        staffService.deleteStaff(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa nhân viên thành công", null));
    }
}
