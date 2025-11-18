package com.medical.post.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "posts", indexes = {
    @Index(name = "idx_doctor_id", columnList = "doctorId"),
    @Index(name = "idx_post_type", columnList = "postType"),
    @Index(name = "idx_created_at", columnList = "createdAt"),
    @Index(name = "idx_specialty", columnList = "specialty")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long doctorId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 5000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostType postType; // CASE_STUDY, RESEARCH, DISCUSSION, NEWS, QUESTION

    private String specialty; // Medical specialty related to the post

    @ElementCollection
    @CollectionTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "post_media_urls", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "media_url", length = 500)
    private List<String> mediaUrls = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "post_paper_urls", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "paper_url", length = 500)
    private List<String> paperUrls = new ArrayList<>();

    // Case study specific fields
    private String patientAge;
    private String patientGender;
    private String diagnosis;
    private String treatment;
    private String outcome;

    // Statistics
    private Long viewCount = 0L;
    private Long likeCount = 0L;
    private Long commentCount = 0L;
    private Long shareCount = 0L;

    private Boolean published = true;
    private Boolean aiAnalyzed = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum PostType {
        CASE_STUDY,
        RESEARCH,
        DISCUSSION,
        NEWS,
        QUESTION
    }
}
