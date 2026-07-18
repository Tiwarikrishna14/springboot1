package com.company.orderapproval.config;

import com.company.orderapproval.organization.entity.Organization;
import com.company.orderapproval.organization.entity.OrganizationStatus;
import com.company.orderapproval.organization.entity.OrganizationType;
import com.company.orderapproval.organization.repository.OrganizationRepository;
import com.company.orderapproval.role.entity.Role;
import com.company.orderapproval.role.repository.RoleRepository;
import com.company.orderapproval.user.entity.User;
import com.company.orderapproval.user.entity.UserRole;
import com.company.orderapproval.user.entity.UserStatus;
import com.company.orderapproval.user.repository.UserRepository;
import com.company.orderapproval.user.repository.UserRoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DevDataSeeder {

    @Bean
    CommandLineRunner seedDevelopmentData(OrganizationRepository organizationRepository,
                                          UserRepository userRepository,
                                          RoleRepository roleRepository,
                                          UserRoleRepository userRoleRepository,
                                          PasswordEncoder passwordEncoder) {
        return args -> {
            Organization demoOrganization = organizationRepository.findByOrganizationCode("DEMO")
                    .orElseGet(() -> {
                        Organization organization = new Organization();
                        organization.setOrganizationCode("DEMO");
                        organization.setName("Demo Organization");
                        organization.setOrganizationType(OrganizationType.CUSTOMER);
                        organization.setStatus(OrganizationStatus.ACTIVE);
                        return organizationRepository.save(organization);
                    });

            User admin = userRepository.findByEmail("admin@example.com")
                    .orElseGet(() -> {
                        User user = new User();
                        user.setOrganizationId(demoOrganization.getId());
                        user.setFirstName("Admin");
                        user.setLastName("User");
                        user.setEmail("admin@example.com");
                        user.setPasswordHash(passwordEncoder.encode("Password@123"));
                        user.setStatus(UserStatus.ACTIVE);
                        user.setEmailVerified(true);
                        return userRepository.save(user);
                    });

            Role organizationAdminRole = roleRepository.findByNameAndOrganizationIdIsNull("ORGANIZATION_ADMIN")
                    .orElseThrow(() -> new IllegalStateException("ORGANIZATION_ADMIN role is missing"));
            if (!userRoleRepository.existsByUserIdAndRoleId(admin.getId(), organizationAdminRole.getId())) {
                UserRole userRole = new UserRole();
                userRole.setUser(admin);
                userRole.setRole(organizationAdminRole);
                userRoleRepository.save(userRole);
            }
        };
    }
}
