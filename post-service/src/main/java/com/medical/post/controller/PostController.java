package com.medical.post.controller;

import com.medical.common.dto.ApiResponse;
import com.medical.common.dto.PageResponse;
import com.medical.post.dto.CreatePostDTO;
import com.medical.post.dto.PostDTO;
import com.medical.post.model.Post;
import com.medical.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<ApiResponse<PostDTO>> createPost(@Valid @RequestBody CreatePostDTO dto) {
        PostDTO post = postService.createPost(dto);
        return ResponseEntity.ok(ApiResponse.success("Post created successfully", post));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDTO>> getPostById(@PathVariable Long id) {
        PostDTO post = postService.getPostById(id);
        return ResponseEntity.ok(ApiResponse.success(post));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PostDTO>>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<PostDTO> posts = postService.getAllPosts(pageable);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<ApiResponse<PageResponse<PostDTO>>> getPostsByDoctor(
            @PathVariable Long doctorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<PostDTO> posts = postService.getPostsByDoctor(doctorId, pageable);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<PageResponse<PostDTO>>> getPostsByType(
            @PathVariable Post.PostType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<PostDTO> posts = postService.getPostsByType(type, pageable);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @GetMapping("/specialty/{specialty}")
    public ResponseEntity<ApiResponse<PageResponse<PostDTO>>> getPostsBySpecialty(
            @PathVariable String specialty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<PostDTO> posts = postService.getPostsBySpecialty(specialty, pageable);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<PostDTO>>> searchPosts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<PostDTO> posts = postService.searchPosts(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<PageResponse<PostDTO>>> getTrendingPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<PostDTO> posts = postService.getTrendingPosts(pageable);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @PostMapping("/feed")
    public ResponseEntity<ApiResponse<PageResponse<PostDTO>>> getFeedPosts(
            @RequestBody List<Long> followingDoctorIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<PostDTO> posts = postService.getFeedPosts(followingDoctorIds, pageable);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDTO>> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody CreatePostDTO dto) {
        PostDTO post = postService.updatePost(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Post updated successfully", post));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success("Post deleted successfully", null));
    }

    @PostMapping("/{postId}/like/{doctorId}")
    public ResponseEntity<ApiResponse<Void>> likePost(
            @PathVariable Long postId,
            @PathVariable Long doctorId) {
        postService.likePost(postId, doctorId);
        return ResponseEntity.ok(ApiResponse.success("Post liked successfully", null));
    }

    @DeleteMapping("/{postId}/unlike/{doctorId}")
    public ResponseEntity<ApiResponse<Void>> unlikePost(
            @PathVariable Long postId,
            @PathVariable Long doctorId) {
        postService.unlikePost(postId, doctorId);
        return ResponseEntity.ok(ApiResponse.success("Post unliked successfully", null));
    }
}
