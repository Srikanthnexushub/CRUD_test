package org.example.repository;

import org.example.entity.RefreshToken;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Refresh Token entity operations.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Find refresh token by token string.
     *
     * @param token the token string
     * @return Optional containing refresh token if found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Find all active (non-revoked, non-expired) tokens for a user.
     *
     * @param user the user
     * @return list of active refresh tokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user " +
           "AND rt.revokedAt IS NULL AND rt.expiresAt > CURRENT_TIMESTAMP " +
           "ORDER BY rt.createdAt DESC")
    List<RefreshToken> findActiveByUser(@Param("user") User user);

    /**
     * Find all tokens for a user (including expired and revoked).
     *
     * @param user the user
     * @return list of all refresh tokens
     */
    List<RefreshToken> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Count active tokens for a user.
     *
     * @param user the user
     * @return count of active tokens
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user " +
           "AND rt.revokedAt IS NULL AND rt.expiresAt > CURRENT_TIMESTAMP")
    long countActiveByUser(@Param("user") User user);

    /**
     * Revoke all active tokens for a user.
     *
     * @param user the user
     * @return number of revoked tokens
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = CURRENT_TIMESTAMP " +
           "WHERE rt.user = :user AND rt.revokedAt IS NULL " +
           "AND rt.expiresAt > CURRENT_TIMESTAMP")
    int revokeAllByUser(@Param("user") User user);

    /**
     * Delete expired or old revoked tokens.
     *
     * @param expirationDate delete tokens expired before this date
     * @param revocationDate delete tokens revoked before this date
     * @return number of deleted tokens
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE " +
           "rt.expiresAt < :expirationDate OR " +
           "(rt.revokedAt IS NOT NULL AND rt.revokedAt < :revocationDate)")
    int deleteExpiredOrOldRevoked(
            @Param("expirationDate") LocalDateTime expirationDate,
            @Param("revocationDate") LocalDateTime revocationDate
    );

    /**
     * Check if token exists and is valid.
     *
     * @param token the token string
     * @return true if token exists and is valid
     */
    @Query("SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END " +
           "FROM RefreshToken rt WHERE rt.token = :token " +
           "AND rt.revokedAt IS NULL AND rt.expiresAt > CURRENT_TIMESTAMP")
    boolean isValidToken(@Param("token") String token);
}
