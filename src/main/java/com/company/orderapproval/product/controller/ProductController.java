package com.company.orderapproval.product.controller;

import com.company.orderapproval.branch.dto.BranchResponse;
import com.company.orderapproval.common.response.ApiResponse;
import com.company.orderapproval.common.response.PageResponse;
import com.company.orderapproval.product.dto.CreateProductRequest;
import com.company.orderapproval.product.dto.ProductResponse;
import com.company.orderapproval.product.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Create single product
     */
    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request) {

        ProductResponse response = productService.createProduct(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

        @PostMapping("/list-products")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> listOfProducts(@RequestParam String customerCode,
             @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable p) {
        PageResponse<ProductResponse> response = PageResponse.from(productService.getProducts(customerCode, p));
        return ResponseEntity.ok(ApiResponse
            .success("Product for this customer fetched successfully", 
            response));

    }

    

    /**
     * Bulk upload products using CSV
     */
    @PostMapping(
            value = "/{customerSellCode}/bulk-upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<String>> bulkUpload(
            @RequestParam("file") MultipartFile file, @PathVariable String customerSellCode) {

        return ResponseEntity.ok(ApiResponse.success("Upload successfully", productService.bulkUploadProducts(customerSellCode, file)));
    }
}
