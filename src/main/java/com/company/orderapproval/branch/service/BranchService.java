package com.company.orderapproval.branch.service;
import com.company.orderapproval.branch.dto.*; import org.springframework.data.domain.*; import jakarta.servlet.http.HttpServletRequest; import java.util.UUID;
public interface BranchService { Page<BranchResponse> list(UUID organizationId, Pageable pageable); BranchResponse get(UUID id); BranchResponse create(UUID organizationId, CreateBranchRequest request, HttpServletRequest http); BranchResponse update(UUID id, UpdateBranchRequest request, HttpServletRequest http); }
