package com.company.orderapproval.product.service;


import com.company.orderapproval.customer.repository.BusinessCustomerRepository;
import com.company.orderapproval.product.dto.CreateProductRequest;
import com.company.orderapproval.product.dto.ProductResponse;
import com.company.orderapproval.product.entity.Product;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final BusinessCustomerRepository businessCustomerRepository;

    @Transactional
    public ProductResponse createProduct(
            CreateProductRequest request) {

        String customerSellCode =
                request.customerSellCode().trim();

        /*
         * Business validation:
         * Customer Sell Code must exist
         * in Business Customer table.
         */
        boolean customerExists =
                businessCustomerRepository
                        .existsByCustomerSellCode(customerSellCode);

        if (!customerExists) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid customer seller code: "
                            + customerSellCode
            );
        }

        /*
         * Check duplicate product
         */
        boolean duplicate =
                productRepository
                        .existsByCustomerSellCodeAndNavItemCode(
                                customerSellCode,
                                request.navItemCode().trim()
                        );

        if (duplicate) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Product already exists for customer seller code "
                            + customerSellCode
                            + " and NAV item code "
                            + request.navItemCode().trim()
            );
        }

        /*
         * Create Product
         */
        Product product = Product.builder()
                .category(request.category().trim())
                .customerSellCode(customerSellCode)
                .navItemCode(request.navItemCode().trim())
                .itemDescription(request.itemDescription().trim())
                .uom(request.uom().trim())
                .unitRate(request.unitRate())
                .status("ACTIVE")
                .build();

        Product savedProduct =
                productRepository.save(product);

        return toResponse(savedProduct);
    }

    private ProductResponse toResponse(Product product) {

        return new ProductResponse(
                product.getId(),
                product.getCategory(),
                product.getCustomerSellCode(),
                product.getNavItemCode(),
                product.getItemDescription(),
                product.getUom(),
                product.getUnitRate(),
                product.getStatus()
        );
    }

        @Transactional
        public String bulkUploadProducts(
                String customerSellCode,
                MultipartFile file
        ) {

            // 1. Validate Customer Seller Code
            boolean customerExists =
                    businessCustomerRepository.existsByCustomerSellCode(
                            customerSellCode
                    );

            if (!customerExists) {
                throw new IllegalArgumentException(
                        "Invalid customer seller code: " + customerSellCode
                );
            }

            // 2. Validate file
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException(
                        "CSV file is mandatory"
                );
            }

            List<Product> products = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            file.getInputStream(),
                            StandardCharsets.UTF_8
                    ))) {

                String line;

                // Skip header
                reader.readLine();

                int rowNumber = 1;

                while ((line = reader.readLine()) != null) {

                    rowNumber++;

                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    String[] columns = line.split(",", -1);

                    if (columns.length != 5) {
                        throw new IllegalArgumentException(
                                "Invalid CSV format at row " + rowNumber
                        );
                    }

                    String category = columns[0].trim();
                    String navItemCode = columns[1].trim();
                    String itemDescription = columns[2].trim();
                    String uom = columns[3].trim();
                    String unitRateValue = columns[4].trim();

                    // 3. Mandatory field validation
                    if (category.isBlank()) {
                        throw new IllegalArgumentException(
                                "Category is mandatory at row " + rowNumber
                        );
                    }

                    if (navItemCode.isBlank()) {
                        throw new IllegalArgumentException(
                                "NAV item code is mandatory at row " + rowNumber
                        );
                    }

                    if (itemDescription.isBlank()) {
                        throw new IllegalArgumentException(
                                "Item description is mandatory at row " + rowNumber
                        );
                    }

                    if (uom.isBlank()) {
                        throw new IllegalArgumentException(
                                "UOM is mandatory at row " + rowNumber
                        );
                    }

                    if (unitRateValue.isBlank()) {
                        throw new IllegalArgumentException(
                                "Unit rate is mandatory at row " + rowNumber
                        );
                    }

                    // 4. Parse Unit Rate
                    BigDecimal unitRate;

                    try {
                        unitRate = new BigDecimal(unitRateValue);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Invalid unit rate at row " + rowNumber
                        );
                    }

                    if (unitRate.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException(
                                "Unit rate must be greater than 0 at row "
                                        + rowNumber
                        );
                    }

                    // 5. Duplicate check
                    boolean productExists =
                            productRepository
                                    .existsByCustomerSellCodeAndNavItemCode(
                                            customerSellCode,
                                            navItemCode
                                    );

                    if (productExists) {
                        throw new IllegalArgumentException(
                                "Product already exists for customer seller code "
                                        + customerSellCode
                                        + " and NAV item code "
                                        + navItemCode
                                        + " at row "
                                        + rowNumber
                        );
                    }

                    // 6. Create Product
                    Product product = Product.builder()
                            .category(category)
                            .customerSellCode(customerSellCode)
                            .navItemCode(navItemCode)
                            .itemDescription(itemDescription)
                            .uom(uom)
                            .unitRate(unitRate)
                            .build();

                    products.add(product);
                }

            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Failed to read CSV file",
                        e
                );
            }

            // 7. Save all products
             productRepository.saveAll(products);

            return "Updated successfully";
        }
    }
