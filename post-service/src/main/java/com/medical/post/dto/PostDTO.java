package com.medical.post.dto;

import com.medical.post.model.Post;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostDTO {
    private Long id;
    private Long doctorId;
    private String doctorName;
    private String doctorSpecialty;
    private String doctorProfileImage;
    private String title;
    private String content;
    private Post.PostType postType;
    private String specialty;
    private Set<String> tags;
    private List<String> mediaUrls;
    private List<String> paperUrls;
    private String patientAge;
    private String patientGender;
    private String diagnosis;
    private String treatment;
    private String outcome;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long shareCount;
    private Boolean aiAnalyzed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
