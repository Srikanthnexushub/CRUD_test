package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "rate_limit_logs", indexes = {
    @Index(name = "idx_ratelimit_user_id", columnList = "user_id"),
    @Index(name = "idx_ratelimit_ip_address", columnList = "ip_address"),
    @Index(name = "idx_ratelimit_created_at", columnList = "created_at"),
    @Index(name = "idx_ratelimit_was_blocked", columnList = "was_blocked"),
    @Index(name = "idx_ratelimit_endpoint", columnList = "endpoint")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "endpoint", nullable = false, length = 200)
    private String endpoint;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "limit_type", nullable = false, length = 20)
    private String limitType; // USER, IP, ENDPOINT

    @Column(name = "current_count", nullable = false)
    private Integer currentCount;

    @Column(name = "limit_threshold", nullable = false)
    private Integer limitThreshold;

    @Column(name = "was_blocked", nullable = false)
    private Boolean wasBlocked = false;

    @Column(name = "reset_time", nullable = false)
    private LocalDateTime resetTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
