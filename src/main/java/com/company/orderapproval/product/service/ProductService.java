package com.company.orderapproval.product.service;

import com.company.orderapproval.common.exception.BadRequestException;
import com.company.orderapproval.common.exception.ResourceNotFoundException;
import com.company.orderapproval.customer.repository.BusinessCustomerRepository;
import com.company.orderapproval.product.dto.CreateProductRequest;
import com.company.orderapproval.product.dto.ProductResponse;
import com.company.orderapproval.product.dto.UpdateProductRequest;
import com.company.orderapproval.product.entity.Product;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final BusinessCustomerRepository businessCustomerRepository;
    private final ProductImageStorageService productImageStorageService;
    private final ObjectProvider<ProductExcelReader> productExcelReaderProvider;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        return createProduct(request, null);
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request, MultipartFile image) {
        String customerSellCode = cleanRequired(request.customerSellCode(), "Customer seller code");
        String navItemCode = cleanRequired(request.navItemCode(), "NAV item code");

        validateCustomer(customerSellCode);
        ensureProductDoesNotExist(customerSellCode, navItemCode, null);

        Product product = Product.builder()
                .category(cleanRequired(request.category(), "Category"))
                .customerSellCode(customerSellCode)
                .navItemCode(navItemCode)
                .itemDescription(cleanRequired(request.itemDescription(), "Item description"))
                .uom(cleanRequired(request.uom(), "UOM"))
                .unitRate(request.unitRate())
                .imagePath(productImageStorageService.store(image))
                .status("ACTIVE")
                .build();

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public String bulkUploadProducts(String customerSellCode, MultipartFile file, List<MultipartFile> images) {
        String normalizedCustomerSellCode = cleanRequired(customerSellCode, "Customer seller code");
        validateCustomer(normalizedCustomerSellCode);

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("CSV or Excel file is mandatory");
        }

        List<BulkProductRow> rows = readBulkProductRows(file);
        if (rows.isEmpty()) {
            throw new BadRequestException("Bulk upload file does not contain product rows");
        }

        Map<String, MultipartFile> imagesByFilename = productImageStorageService.indexByOriginalFilename(images);
        Map<String, String> storedImagePaths = new HashMap<>();
        Set<String> uploadedProductKeys = new HashSet<>();
        List<Product> products = new ArrayList<>();

        for (BulkProductRow row : rows) {
            String category = cleanRequired(row.category(), "Category", row.rowNumber());
            String navItemCode = cleanRequired(row.navItemCode(), "NAV item code", row.rowNumber());
            String itemDescription = cleanRequired(row.itemDescription(), "Item description", row.rowNumber());
            String uom = cleanRequired(row.uom(), "UOM", row.rowNumber());
            BigDecimal unitRate = parseUnitRate(row.unitRateValue(), row.rowNumber());

            String productKey = normalizedCustomerSellCode + "|" + navItemCode.toUpperCase(Locale.ROOT);
            if (!uploadedProductKeys.add(productKey)) {
                throw new BadRequestException("Duplicate NAV item code in bulk upload at row " + row.rowNumber());
            }
            ensureProductDoesNotExist(normalizedCustomerSellCode, navItemCode, row.rowNumber());

            Product product = Product.builder()
                    .category(category)
                    .customerSellCode(normalizedCustomerSellCode)
                    .navItemCode(navItemCode)
                    .itemDescription(itemDescription)
                    .uom(uom)
                    .unitRate(unitRate)
                    .imagePath(resolveBulkImagePath(row, imagesByFilename, storedImagePaths))
                    .status("ACTIVE")
                    .build();

            products.add(product);
        }

        productRepository.saveAll(products);
        return "Updated successfully";
    }

    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        return updateProduct(id, request, null);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request, MultipartFile image) {
        Product product = findProduct(id);
        String customerSellCode = cleanRequired(request.customerSellCode(), "Customer seller code");
        String navItemCode = cleanRequired(request.navItemCode(), "NAV item code");

        validateCustomer(customerSellCode);
        ensureProductDoesNotExistForUpdate(customerSellCode, navItemCode, id);

        String oldImagePath = product.getImagePath();
        String newImagePath = oldImagePath;
        String storedImagePath = productImageStorageService.store(image);
        if (storedImagePath != null) {
            newImagePath = storedImagePath;
        } else if (Boolean.TRUE.equals(request.removeImage())) {
            newImagePath = null;
        }

        product.setCategory(cleanRequired(request.category(), "Category"));
        product.setCustomerSellCode(customerSellCode);
        product.setNavItemCode(navItemCode);
        product.setItemDescription(cleanRequired(request.itemDescription(), "Item description"));
        product.setUom(cleanRequired(request.uom(), "UOM"));
        product.setUnitRate(request.unitRate());
        product.setStatus(cleanRequired(request.status(), "Status").toUpperCase(Locale.ROOT));
        product.setImagePath(newImagePath);

        Product savedProduct = productRepository.save(product);
        if (oldImagePath != null && !oldImagePath.equals(newImagePath)) {
            productImageStorageService.delete(oldImagePath);
        }

        return toResponse(savedProduct);
    }

    @Transactional
    public int deleteProducts(List<Long> ids) {
        List<Long> productIds = normalizeProductIds(ids);
        List<Product> products = productRepository.findAllById(productIds);

        if (products.size() != productIds.size()) {
            Set<Long> foundIds = new HashSet<>();
            for (Product product : products) {
                foundIds.add(product.getId());
            }

            List<Long> missingIds = productIds.stream()
                    .filter(productId -> !foundIds.contains(productId))
                    .toList();
            throw new ResourceNotFoundException("Products not found: " + missingIds);
        }

        productRepository.deleteAll(products);
        for (Product product : products) {
            productImageStorageService.delete(product.getImagePath());
        }

        return products.size();
    }

    public Page<ProductResponse> getProducts(String customerCode, Pageable pageable) {
        Page<Product> products = productRepository.findByCustomerSellCode(customerCode, pageable);
        return products.map(this::toResponse);
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
                product.getImagePath(),
                product.getStatus()
        );
    }

    private void validateCustomer(String customerSellCode) {
        if (!businessCustomerRepository.existsByCustomerCode(customerSellCode)) {
            throw new BadRequestException("Invalid customer seller code: " + customerSellCode);
        }
    }

    private Product findProduct(Long id) {
        if (id == null) {
            throw new BadRequestException("Product id is mandatory");
        }
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private void ensureProductDoesNotExist(String customerSellCode, String navItemCode, Integer rowNumber) {
        boolean duplicate = productRepository.existsByCustomerSellCodeAndNavItemCode(customerSellCode, navItemCode);
        if (duplicate) {
            String message = "Product already exists for customer seller code "
                    + customerSellCode
                    + " and NAV item code "
                    + navItemCode;
            if (rowNumber != null) {
                message += " at row " + rowNumber;
            }
            throw new BadRequestException(message);
        }
    }

    private void ensureProductDoesNotExistForUpdate(String customerSellCode, String navItemCode, Long productId) {
        boolean duplicate = productRepository.existsByCustomerSellCodeAndNavItemCodeAndIdNot(
                customerSellCode,
                navItemCode,
                productId
        );
        if (duplicate) {
            throw new BadRequestException(
                    "Product already exists for customer seller code "
                            + customerSellCode
                            + " and NAV item code "
                            + navItemCode
            );
        }
    }

    private String resolveBulkImagePath(
            BulkProductRow row,
            Map<String, MultipartFile> imagesByFilename,
            Map<String, String> storedImagePaths
    ) {
        String imageFilename = cleanOptional(row.imageFilename());
        if (imageFilename == null) {
            return null;
        }

        String imageKey = productImageStorageService.filenameKey(imageFilename);
        MultipartFile image = imagesByFilename.get(imageKey);
        if (image == null) {
            throw new BadRequestException(
                    "Image file '" + imageFilename + "' referenced at row " + row.rowNumber() + " was not uploaded"
            );
        }

        return storedImagePaths.computeIfAbsent(imageKey, ignored -> productImageStorageService.store(image));
    }

    private List<BulkProductRow> readBulkProductRows(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            ProductExcelReader productExcelReader = productExcelReaderProvider.getIfAvailable();
            if (productExcelReader == null) {
                throw new BadRequestException("Excel upload support is not available. Please run the app with Maven dependencies refreshed or upload CSV.");
            }
            return productExcelReader.readRows(file);
        }
        if (filename.endsWith(".csv") || "text/csv".equalsIgnoreCase(file.getContentType())) {
            return readCsvRows(file);
        }
        throw new BadRequestException("Bulk upload file must be CSV, XLS, or XLSX");
    }

    private List<BulkProductRow> readCsvRows(MultipartFile file) {
        List<BulkProductRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
        )) {
            reader.readLine();
            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                List<String> columns = parseCsvLine(line, rowNumber);
                if (columns.size() < 5 || columns.size() > 6) {
                    throw new BadRequestException("Invalid CSV format at row " + rowNumber);
                }

                rows.add(new BulkProductRow(
                        columns.get(0),
                        columns.get(1),
                        columns.get(2),
                        columns.get(3),
                        columns.get(4),
                        columns.size() == 6 ? columns.get(5) : null,
                        rowNumber
                ));
            }
        } catch (IOException ex) {
            throw new BadRequestException("Failed to read CSV file");
        }
        return rows;
    }

    private List<String> parseCsvLine(String line, int rowNumber) {
        List<String> columns = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                columns.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }

        if (quoted) {
            throw new BadRequestException("Invalid CSV quote at row " + rowNumber);
        }

        columns.add(current.toString().trim());
        return columns;
    }

    private BigDecimal parseUnitRate(String unitRateValue, int rowNumber) {
        String normalizedUnitRate = cleanRequired(unitRateValue, "Unit rate", rowNumber).replace(",", "");
        try {
            BigDecimal unitRate = new BigDecimal(normalizedUnitRate);
            if (unitRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Unit rate must be greater than 0 at row " + rowNumber);
            }
            return unitRate;
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Invalid unit rate at row " + rowNumber);
        }
    }

    private String cleanRequired(String value, String fieldName) {
        String cleaned = cleanOptional(value);
        if (cleaned == null) {
            throw new BadRequestException(fieldName + " is mandatory");
        }
        return cleaned;
    }

    private String cleanRequired(String value, String fieldName, int rowNumber) {
        String cleaned = cleanOptional(value);
        if (cleaned == null) {
            throw new BadRequestException(fieldName + " is mandatory at row " + rowNumber);
        }
        return cleaned;
    }

    private String cleanOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private List<Long> normalizeProductIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("Product ids are mandatory");
        }

        List<Long> productIds = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        for (Long id : ids) {
            if (id == null) {
                throw new BadRequestException("Product id cannot be null");
            }
            if (id <= 0) {
                throw new BadRequestException("Product id must be greater than 0");
            }
            if (seenIds.add(id)) {
                productIds.add(id);
            }
        }
        return productIds;
    }

}
