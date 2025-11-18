package com.medical.post.dto;

import com.medical.post.model.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostDTO {

    @NotNull
    private Long doctorId;

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must be less than 500 characters")
    private String title;

    @Size(max = 5000, message = "Content must be less than 5000 characters")
    private String content;

    @NotNull(message = "Post type is required")
    private Post.PostType postType;

    private String specialty;
    private Set<String> tags;
    private List<String> mediaUrls;
    private List<String> paperUrls;

    // Case study fields
    private String patientAge;
    private String patientGender;
    private String diagnosis;
    private String treatment;
    private String outcome;
}
