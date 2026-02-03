package org.example.repository;

import org.example.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Login Attempt entity operations.
 */
@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    /**
     * Count recent failed login attempts by username.
     *
     * @param username the username
     * @param since timestamp to count from
     * @return count of failed attempts
     */
    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.username = :username " +
           "AND la.success = false AND la.createdAt > :since")
    long countFailedAttemptsByUsername(@Param("username") String username, @Param("since") LocalDateTime since);

    /**
     * Count recent failed login attempts by IP address.
     *
     * @param ipAddress the IP address
     * @param since timestamp to count from
     * @return count of failed attempts
     */
    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.ipAddress = :ipAddress " +
           "AND la.success = false AND la.createdAt > :since")
    long countFailedAttemptsByIp(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);

    /**
     * Find recent failed login attempts by username.
     *
     * @param username the username
     * @param since timestamp to search from
     * @return list of failed attempts
     */
    @Query("SELECT la FROM LoginAttempt la WHERE la.username = :username " +
           "AND la.success = false AND la.createdAt > :since " +
           "ORDER BY la.createdAt DESC")
    List<LoginAttempt> findRecentFailedAttemptsByUsername(
            @Param("username") String username,
            @Param("since") LocalDateTime since
    );

    /**
     * Find recent failed login attempts by IP address.
     *
     * @param ipAddress the IP address
     * @param since timestamp to search from
     * @return list of failed attempts
     */
    @Query("SELECT la FROM LoginAttempt la WHERE la.ipAddress = :ipAddress " +
           "AND la.success = false AND la.createdAt > :since " +
           "ORDER BY la.createdAt DESC")
    List<LoginAttempt> findRecentFailedAttemptsByIp(
            @Param("ipAddress") String ipAddress,
            @Param("since") LocalDateTime since
    );

    /**
     * Find all login attempts for a username.
     *
     * @param username the username
     * @return list of all attempts
     */
    List<LoginAttempt> findByUsernameOrderByCreatedAtDesc(String username);

    /**
     * Find successful login attempts by username within time range.
     *
     * @param username the username
     * @param since timestamp to search from
     * @return list of successful attempts
     */
    @Query("SELECT la FROM LoginAttempt la WHERE la.username = :username " +
           "AND la.success = true AND la.createdAt > :since " +
           "ORDER BY la.createdAt DESC")
    List<LoginAttempt> findSuccessfulAttemptsByUsername(
            @Param("username") String username,
            @Param("since") LocalDateTime since
    );

    /**
     * Delete old login attempts (cleanup).
     *
     * @param before delete attempts older than this timestamp
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.createdAt < :before")
    int deleteOldAttempts(@Param("before") LocalDateTime before);

    /**
     * Check if user has any recent successful logins.
     *
     * @param username the username
     * @param since timestamp to check from
     * @return true if user has successful login
     */
    @Query("SELECT CASE WHEN COUNT(la) > 0 THEN true ELSE false END " +
           "FROM LoginAttempt la WHERE la.username = :username " +
           "AND la.success = true AND la.createdAt > :since")
    boolean hasRecentSuccessfulLogin(@Param("username") String username, @Param("since") LocalDateTime since);
}
