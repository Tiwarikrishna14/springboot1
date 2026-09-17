package com.company.orderapproval.auth.repository;

import com.company.orderapproval.auth.security.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from PasswordResetToken token where token.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
