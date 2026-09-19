package com.company.orderapproval.product.service;

import com.company.orderapproval.common.exception.BadRequestException;
import com.company.orderapproval.customer.entity.BusinessCustomer;
import com.company.orderapproval.product.entity.Product;
import com.company.orderapproval.product.entity.ProductCustomerMapping;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductBulkBatchWriter {
    private final ProductRepository products;
    private final ProductCustomerMappingRepository mappings;
    public ProductBulkBatchWriter(ProductRepository products, ProductCustomerMappingRepository mappings) {
        this.products = products; this.mappings = mappings;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(BusinessCustomer customer, List<PreparedBulkProduct> rows) {
        if (rows.isEmpty()) return;
        Set<String> codes = rows.stream().map(row -> row.navItemCode().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> mappedCodes = new HashSet<>(mappings.findMappedNavCodes(customer.getId(), codes));
        rows.stream().filter(row -> mappedCodes.contains(row.navItemCode().toUpperCase(Locale.ROOT))).findFirst()
                .ifPresent(row -> { throw new BadRequestException("Product already exists for customer seller code "
                        + customer.getCustomerCode() + " and NAV item code " + row.navItemCode()
                        + " at row " + row.rowNumber()); });

        Map<String, Product> byCode = products.findByNavItemCodes(codes).stream()
                .collect(Collectors.toMap(p -> p.getNavItemCode().toUpperCase(Locale.ROOT), p -> p));
        List<Product> newProducts = rows.stream()
                .filter(row -> !byCode.containsKey(row.navItemCode().toUpperCase(Locale.ROOT)))
                .map(row -> Product.builder().category(row.category()).navItemCode(row.navItemCode())
                        .itemDescription(row.itemDescription()).uom(row.uom()).unitRate(row.unitRate())
                        .imagePath(row.imagePath()).status("ACTIVE").build()).toList();
        products.saveAll(newProducts);
        products.flush();
        newProducts.forEach(product -> byCode.put(product.getNavItemCode().toUpperCase(Locale.ROOT), product));

        List<ProductCustomerMapping> batch = rows.stream().map(row -> ProductCustomerMapping.builder()
                .businessCustomer(customer)
                .product(byCode.get(row.navItemCode().toUpperCase(Locale.ROOT)))
                .productName(row.itemDescription()).status("ACTIVE").build()).toList();
        mappings.saveAllAndFlush(batch);
    }
}
