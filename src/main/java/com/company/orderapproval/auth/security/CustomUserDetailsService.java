package com.company.orderapproval.auth.security;

import com.company.orderapproval.common.exception.ResourceNotFoundException;
import com.company.orderapproval.user.entity.User;
import com.company.orderapproval.user.repository.UserRepository;
import com.company.orderapproval.user.repository.UserRoleRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public CustomUserDetailsService(UserRepository userRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        AuthenticatedUser principal = loadPrincipal(user);
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .disabled(!user.getStatus().name().equals("ACTIVE"))
                .accountLocked(user.isLockedNow())
                .authorities(principal.authorities())
                .build();
    }

    public AuthenticatedUser loadPrincipalByUserId(java.util.UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return loadPrincipal(user);
    }

    public AuthenticatedUser loadPrincipal(User user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getOrganizationId(),
                user.getEmail(),
                userRoleRepository.findRoleNamesByUserId(user.getId()),
                userRoleRepository.findPermissionCodesByUserId(user.getId())
        );
    }
}
