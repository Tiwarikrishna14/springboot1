package com.company.orderapproval.product.controller;

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
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.ArrayList;
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
    @PostMapping(value = "/products", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request) {

        ProductResponse response = productService.createProduct(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> createProductWithImage(
            @Valid @ModelAttribute CreateProductRequest request,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        ProductResponse response = productService.createProduct(request, image);

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
            MultipartHttpServletRequest request,
            @PathVariable String customerSellCode) {

        MultipartFile file = request.getFile("file");
        List<MultipartFile> images = new ArrayList<>(request.getFiles("images"));
        images.addAll(request.getFiles("images[]"));

        return ResponseEntity.ok(ApiResponse.success("Upload successfully", productService.bulkUploadProducts(customerSellCode, file, images)));
    }
}
