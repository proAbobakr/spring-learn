package com.medical.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "followings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"follower_id", "following_id"})
}, indexes = {
    @Index(name = "idx_follower", columnList = "follower_id"),
    @Index(name = "idx_following", columnList = "following_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Following {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "following_id", nullable = false)
    private Long followingId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
