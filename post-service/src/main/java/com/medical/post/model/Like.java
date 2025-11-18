package com.medical.post.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"post_id", "doctor_id"})
}, indexes = {
    @Index(name = "idx_post_id", columnList = "post_id"),
    @Index(name = "idx_doctor_id", columnList = "doctor_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
