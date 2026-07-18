package com.company.orderapproval.user.controller;

import com.company.orderapproval.common.response.ApiResponse;
import com.company.orderapproval.common.response.PageResponse;
import com.company.orderapproval.user.dto.AssignRolesRequest;
import com.company.orderapproval.user.dto.CreateUserRequest;
import com.company.orderapproval.user.dto.UpdateUserRequest;
import com.company.orderapproval.user.dto.UpdateUserStatusRequest;
import com.company.orderapproval.user.dto.UserResponse;
import com.company.orderapproval.user.service.UserService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "List users")
    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Users fetched successfully",
                PageResponse.from(userService.list(search, pageable))
        ));
    }

    @Operation(summary = "Get user by id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<ApiResponse<UserResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("User fetched successfully", userService.get(id)));
    }

    @Operation(summary = "Create user")
    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request,
                                                            HttpServletRequest servletRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", userService.create(request, servletRequest)));
    }

    @Operation(summary = "Update user")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable UUID id,
                                                            @Valid @RequestBody UpdateUserRequest request,
                                                            HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", userService.update(id, request, servletRequest)));
    }

    @Operation(summary = "Update user status")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_DISABLE')")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(@PathVariable UUID id,
                                                                  @Valid @RequestBody UpdateUserStatusRequest request,
                                                                  HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                "User status updated successfully",
                userService.updateStatus(id, request, servletRequest)
        ));
    }

    @Operation(summary = "Assign roles to user")
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ROLE_ASSIGN')")
    public ResponseEntity<ApiResponse<UserResponse>> assignRoles(@PathVariable UUID id,
                                                                 @Valid @RequestBody AssignRolesRequest request,
                                                                 HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success("Roles assigned successfully", userService.assignRoles(id, request, servletRequest)));
    }

    @Operation(summary = "Remove role from user")
    @DeleteMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasAuthority('ROLE_ASSIGN')")
    public ResponseEntity<ApiResponse<UserResponse>> removeRole(@PathVariable UUID id,
                                                                @PathVariable UUID roleId,
                                                                HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success("Role removed successfully", userService.removeRole(id, roleId, servletRequest)));
    }
}
