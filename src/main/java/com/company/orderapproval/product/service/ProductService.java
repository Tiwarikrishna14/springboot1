package com.company.orderapproval.product.service;

import com.company.orderapproval.common.exception.BadRequestException;
import com.company.orderapproval.common.exception.ResourceNotFoundException;
import com.company.orderapproval.customer.repository.BusinessCustomerRepository;
import com.company.orderapproval.customer.entity.BusinessCustomer;
import com.company.orderapproval.product.dto.CreateProductRequest;
import com.company.orderapproval.product.dto.ProductResponse;
import com.company.orderapproval.product.dto.UpdateProductRequest;
import com.company.orderapproval.product.entity.Product;
import com.company.orderapproval.product.entity.ProductCustomerMapping;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCustomerMappingRepository mappingRepository;
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

        BusinessCustomer customer = customer(customerSellCode);
        ensureMappingDoesNotExist(customer.getId(), customerSellCode, navItemCode, null);
        Product product = productRepository.findByNavItemCodeIgnoreCase(navItemCode)
                .orElseGet(() -> productRepository.save(Product.builder()
                        .category(cleanRequired(request.category(), "Category"))
                        .navItemCode(navItemCode)
                        .itemDescription(cleanRequired(request.itemDescription(), "Item description"))
                        .uom(cleanRequired(request.uom(), "UOM"))
                        .unitRate(request.unitRate())
                        .imagePath(productImageStorageService.store(image))
                        .status("ACTIVE")
                        .build()));

        ProductCustomerMapping mapping = mapping(customer, product, cleanRequired(request.itemDescription(), "Item description"), "ACTIVE");
        return toResponse(mappingRepository.save(mapping), customerSellCode);
    }

    @Transactional
    public String bulkUploadProducts(String customerSellCode, MultipartFile file, List<MultipartFile> images) {
        return String.valueOf(processBulkUpload(customerSellCode, file, images, (total, processed) -> {}));
    }

    public int processBulkUpload(String customerSellCode, byte[] fileBytes, String fileName, String contentType,
                                 List<BulkUploadImage> images, java.util.function.BiConsumer<Integer, Integer> progress) {
        MultipartFile file = new ByteArrayMultipartFile("file", fileName, contentType, fileBytes);
        List<MultipartFile> multipartImages = images.stream()
                .map(image -> (MultipartFile) new ByteArrayMultipartFile("images", image.fileName(), image.contentType(), image.bytes()))
                .toList();
        return processBulkUpload(customerSellCode, file, multipartImages, progress);
    }

    private int processBulkUpload(String customerSellCode, MultipartFile file, List<MultipartFile> images,
                                  java.util.function.BiConsumer<Integer, Integer> progress) {
        String normalizedCustomerSellCode = cleanRequired(customerSellCode, "Customer seller code");
        BusinessCustomer customer = customer(normalizedCustomerSellCode);

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("CSV or Excel file is mandatory");
        }

        List<BulkProductRow> rows = readBulkProductRows(file);
        if (rows.isEmpty()) {
            throw new BadRequestException("Bulk upload file does not contain product rows");
        }
        // The Excel/CSV row count is available before product validation and insertion.
        // Publish it early so the client can render a meaningful 0% of N progress state.
        progress.accept(rows.size(), 0);

        Map<String, MultipartFile> imagesByFilename = productImageStorageService.indexByOriginalFilename(images);
        Map<String, String> storedImagePaths = new HashMap<>();
        Set<String> uploadedProductKeys = new HashSet<>();
        List<ProductCustomerMapping> mappings = new ArrayList<>();

        for (BulkProductRow row : rows) {
            String category = cleanRequired(row.category(), "Category", row.rowNumber());
            String navItemCode = cleanRequired(row.navItemCode(), "NAV item code", row.rowNumber());
            String itemDescription = cleanRequired(row.itemDescription(), "Item description", row.rowNumber());
            String uom = cleanRequired(row.uom(), "UOM", row.rowNumber());
            BigDecimal unitRate = parseUnitRate(row.unitRateValue(), row.rowNumber());

            String productKey = navItemCode.toUpperCase(Locale.ROOT);
            if (!uploadedProductKeys.add(productKey)) {
                throw new BadRequestException("Duplicate NAV item code in bulk upload at row " + row.rowNumber());
            }
            ensureMappingDoesNotExist(customer.getId(), normalizedCustomerSellCode, navItemCode, row.rowNumber());
            Product product = productRepository.findByNavItemCodeIgnoreCase(navItemCode).orElseGet(() ->
                    productRepository.save(Product.builder()
                            .category(category)
                            .navItemCode(navItemCode)
                            .itemDescription(itemDescription)
                            .uom(uom)
                            .unitRate(unitRate)
                            .imagePath(resolveBulkImagePath(row, imagesByFilename, storedImagePaths))
                            .status("ACTIVE")
                            .build()));
            mappings.add(mapping(customer, product, itemDescription, "ACTIVE"));
        }

        for (int start = 0; start < mappings.size(); start += 100) {
            mappingRepository.saveAll(mappings.subList(start, Math.min(start + 100, mappings.size())));
            progress.accept(mappings.size(), Math.min(start + 100, mappings.size()));
        }
        return mappings.size();
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

        BusinessCustomer customer = customer(customerSellCode);
        ProductCustomerMapping mapping = mappingRepository.findByProductIdAndBusinessCustomerId(id, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product is not mapped to this customer"));
        Product existingMaster = productRepository.findByNavItemCodeIgnoreCase(navItemCode).orElse(product);

        String oldImagePath = existingMaster.getImagePath();
        String newImagePath = oldImagePath;
        String storedImagePath = product == existingMaster ? productImageStorageService.store(image) : null;
        if (storedImagePath != null) {
            newImagePath = storedImagePath;
        } else if (Boolean.TRUE.equals(request.removeImage())) {
            newImagePath = null;
        }

        existingMaster.setCategory(cleanRequired(request.category(), "Category"));
        existingMaster.setNavItemCode(navItemCode);
        existingMaster.setItemDescription(cleanRequired(request.itemDescription(), "Item description"));
        existingMaster.setUom(cleanRequired(request.uom(), "UOM"));
        existingMaster.setUnitRate(request.unitRate());
        existingMaster.setImagePath(newImagePath);
        mapping.setProduct(existingMaster);
        mapping.setProductName(cleanRequired(request.itemDescription(), "Item description"));
        mapping.setStatus(cleanRequired(request.status(), "Status").toUpperCase(Locale.ROOT));

        productRepository.save(existingMaster);
        mappingRepository.save(mapping);
        if (oldImagePath != null && !oldImagePath.equals(newImagePath)) {
            productImageStorageService.delete(oldImagePath);
        }

        return toResponse(mapping, customerSellCode);
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

        mappingRepository.deleteByProductIds(productIds);
        productRepository.deleteAll(products);
        for (Product product : products) {
            productImageStorageService.delete(product.getImagePath());
        }

        return products.size();
    }

    public Page<ProductResponse> getProducts(String customerCode, Pageable pageable) {
        Page<ProductCustomerMapping> mappings = mappingRepository.findByBusinessCustomerCustomerCodeIgnoreCase(customerCode, pageable);
        return mappings.map(mapping -> toResponse(mapping, customerCode));
    }

    private ProductResponse toResponse(ProductCustomerMapping mapping, String customerCode) {
        Product product = mapping.getProduct();
        return new ProductResponse(
                product.getId(),
                product.getCategory(),
                customerCode,
                product.getNavItemCode(),
                mapping.getProductName(),
                product.getUom(),
                product.getUnitRate(),
                product.getImagePath(),
                mapping.getStatus()
        );
    }

    private BusinessCustomer customer(String customerSellCode) {
        return businessCustomerRepository.findByCustomerCodeIgnoreCase(customerSellCode)
                .orElseThrow(() -> new BadRequestException("Invalid customer seller code: " + customerSellCode));
    }

    private Product findProduct(Long id) {
        if (id == null) {
            throw new BadRequestException("Product id is mandatory");
        }
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private void ensureMappingDoesNotExist(UUID customerId, String customerCode, String navItemCode, Integer rowNumber) {
        boolean duplicate = mappingRepository.existsByBusinessCustomerIdAndProductNavItemCodeIgnoreCase(customerId, navItemCode);
        if (duplicate) {
            String message = "Product already exists for customer seller code "
                    + customerCode
                    + " and NAV item code "
                    + navItemCode;
            if (rowNumber != null) {
                message += " at row " + rowNumber;
            }
            throw new BadRequestException(message);
        }
    }

    private ProductCustomerMapping mapping(BusinessCustomer customer, Product product, String productName, String status) {
        return ProductCustomerMapping.builder().businessCustomer(customer).product(product).productName(productName).status(status).build();
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
