package com.company.orderapproval.role.controller;

import com.company.orderapproval.common.response.ApiResponse;
import com.company.orderapproval.common.response.PageResponse;
import com.company.orderapproval.permission.dto.PermissionResponse;
import com.company.orderapproval.permission.service.PermissionService;
import com.company.orderapproval.role.dto.AssignPermissionsRequest;
import com.company.orderapproval.role.dto.CreateRoleRequest;
import com.company.orderapproval.role.dto.RoleResponse;
import com.company.orderapproval.role.dto.UpdateRoleRequest;
import com.company.orderapproval.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class RoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;

    public RoleController(RoleService roleService, PermissionService permissionService) {
        this.roleService = roleService;
        this.permissionService = permissionService;
    }

    @Operation(summary = "List roles")
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<RoleResponse>>> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Roles fetched successfully",
                PageResponse.from(roleService.list(search, pageable))
        ));
    }

    @Operation(summary = "Get role by id")
    @GetMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public ResponseEntity<ApiResponse<RoleResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Role fetched successfully", roleService.get(id)));
    }

    @Operation(summary = "Create role")
    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public ResponseEntity<ApiResponse<RoleResponse>> create(@Valid @RequestBody CreateRoleRequest request,
                                                            HttpServletRequest servletRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Role created successfully", roleService.create(request, servletRequest)));
    }

    @Operation(summary = "Update role")
    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<ApiResponse<RoleResponse>> update(@PathVariable UUID id,
                                                            @Valid @RequestBody UpdateRoleRequest request,
                                                            HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success("Role updated successfully", roleService.update(id, request, servletRequest)));
    }

    @Operation(summary = "Assign permissions to role")
    @PostMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<ApiResponse<RoleResponse>> assignPermissions(@PathVariable UUID id,
                                                                       @Valid @RequestBody AssignPermissionsRequest request,
                                                                       HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                "Permissions assigned successfully",
                roleService.assignPermissions(id, request, servletRequest)
        ));
    }

    @Operation(summary = "Remove permission from role")
    @DeleteMapping("/roles/{id}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<ApiResponse<RoleResponse>> removePermission(@PathVariable UUID id,
                                                                      @PathVariable UUID permissionId,
                                                                      HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                "Permission removed successfully",
                roleService.removePermission(id, permissionId, servletRequest)
        ));
    }

    @Operation(summary = "List permissions")
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> permissions() {
        return ResponseEntity.ok(ApiResponse.success("Permissions fetched successfully", permissionService.list()));
    }
}
