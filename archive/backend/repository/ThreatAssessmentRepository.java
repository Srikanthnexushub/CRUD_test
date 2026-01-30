package org.example.repository;

import org.example.entity.ThreatAssessment;
import org.example.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ThreatAssessment entity operations.
 *
 * DEBUGGING GUIDE:
 * ----------------
 * To debug threat intelligence queries:
 * 1. Set breakpoints in RepositoryLoggingAspect.java (see line numbers in comments)
 * 2. Watch for performance issues on complex date range queries
 * 3. Monitor execution time logged by RepositoryLoggingAspect
 *
 * BREAKPOINT LOCATIONS:
 * - Line 32: findByUserOrderByAssessedAtDesc() - Debug user threat history queries
 * - Line 35: findByUserAndAssessedAtAfterOrderByAssessedAtDesc() - Debug recent threats for specific user
 * - Line 39: findHighRiskAssessments() - Debug high-risk threat detection (custom JPQL)
 * - Line 43: findByDateRange() - Debug date-based threat analysis (custom JPQL)
 * - Line 47: countRecentAssessments() - Debug threat count aggregation (custom JPQL)
 */
@Repository
public interface ThreatAssessmentRepository extends JpaRepository<ThreatAssessment, Long> {

    // BREAKPOINT: Debug user-specific threat history with pagination
    Page<ThreatAssessment> findByUserOrderByAssessedAtDesc(User user, Pageable pageable);

    // BREAKPOINT: Debug recent threats for a user (time-based filtering)
    List<ThreatAssessment> findByUserAndAssessedAtAfterOrderByAssessedAtDesc(User user, LocalDateTime after);

    // BREAKPOINT: Debug high-risk threat queries (JPQL with score threshold)
    // Watch SQL: WHERE ta.risk_score >= ? ORDER BY ta.assessed_at DESC
    @Query("SELECT ta FROM ThreatAssessment ta WHERE ta.riskScore >= :minScore ORDER BY ta.assessedAt DESC")
    Page<ThreatAssessment> findHighRiskAssessments(Integer minScore, Pageable pageable);

    // BREAKPOINT: Debug date range queries (JPQL with BETWEEN-like logic)
    // Watch SQL: WHERE ta.assessed_at >= ? AND ta.assessed_at < ?
    @Query("SELECT ta FROM ThreatAssessment ta WHERE ta.assessedAt >= :startDate AND ta.assessedAt < :endDate ORDER BY ta.assessedAt DESC")
    List<ThreatAssessment> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    // BREAKPOINT: Debug count aggregation queries (performance monitoring)
    // Watch SQL: SELECT COUNT(*) FROM threat_assessments WHERE user_id = ? AND assessed_at > ?
    @Query("SELECT COUNT(ta) FROM ThreatAssessment ta WHERE ta.user = :user AND ta.assessedAt > :since")
    long countRecentAssessments(User user, LocalDateTime since);
}
