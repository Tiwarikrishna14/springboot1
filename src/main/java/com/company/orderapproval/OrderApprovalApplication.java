package com.company.orderapproval;

import com.company.orderapproval.auth.security.JwtProperties;
import com.company.orderapproval.config.CorsProperties;
import com.company.orderapproval.config.ProductImageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class, ProductImageProperties.class})
public class OrderApprovalApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApprovalApplication.class, args);
    }
}
