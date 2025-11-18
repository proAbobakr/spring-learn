package com.medical.post.service;

import com.medical.common.dto.PageResponse;
import com.medical.common.exception.BadRequestException;
import com.medical.common.exception.ResourceNotFoundException;
import com.medical.post.dto.CreatePostDTO;
import com.medical.post.dto.PostDTO;
import com.medical.post.model.Like;
import com.medical.post.model.Post;
import com.medical.post.repository.LikeRepository;
import com.medical.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    @CacheEvict(value = "posts", allEntries = true)
    public PostDTO createPost(CreatePostDTO dto) {
        Post post = new Post();
        post.setDoctorId(dto.getDoctorId());
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setPostType(dto.getPostType());
        post.setSpecialty(dto.getSpecialty());
        post.setTags(dto.getTags());
        post.setMediaUrls(dto.getMediaUrls());
        post.setPaperUrls(dto.getPaperUrls());
        post.setPatientAge(dto.getPatientAge());
        post.setPatientGender(dto.getPatientGender());
        post.setDiagnosis(dto.getDiagnosis());
        post.setTreatment(dto.getTreatment());
        post.setOutcome(dto.getOutcome());

        post = postRepository.save(post);

        // Publish event
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "POST_CREATED");
        event.put("postId", post.getId());
        event.put("doctorId", post.getDoctorId());
        event.put("postType", post.getPostType());
        kafkaTemplate.send("post-events", event);

        // Trigger AI analysis if it's a case study with media
        if (post.getPostType() == Post.PostType.CASE_STUDY &&
            post.getMediaUrls() != null && !post.getMediaUrls().isEmpty()) {
            Map<String, Object> aiEvent = new HashMap<>();
            aiEvent.put("eventType", "AI_ANALYSIS_REQUESTED");
            aiEvent.put("postId", post.getId());
            aiEvent.put("mediaUrls", post.getMediaUrls());
            kafkaTemplate.send("ai-events", aiEvent);
        }

        return convertToDTO(post);
    }

    @Cacheable(value = "posts", key = "#id")
    public PostDTO getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        // Increment view count
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);

        return convertToDTO(post);
    }

    public PageResponse<PostDTO> getAllPosts(Pageable pageable) {
        Page<Post> page = postRepository.findAllPublished(pageable);
        return convertToPageResponse(page);
    }

    public PageResponse<PostDTO> getPostsByDoctor(Long doctorId, Pageable pageable) {
        Page<Post> page = postRepository.findByDoctorId(doctorId, pageable);
        return convertToPageResponse(page);
    }

    public PageResponse<PostDTO> getPostsByType(Post.PostType type, Pageable pageable) {
        Page<Post> page = postRepository.findByPostType(type, pageable);
        return convertToPageResponse(page);
    }

    public PageResponse<PostDTO> getPostsBySpecialty(String specialty, Pageable pageable) {
        Page<Post> page = postRepository.findBySpecialty(specialty, pageable);
        return convertToPageResponse(page);
    }

    public PageResponse<PostDTO> searchPosts(String query, Pageable pageable) {
        Page<Post> page = postRepository.searchPosts(query, pageable);
        return convertToPageResponse(page);
    }

    public PageResponse<PostDTO> getTrendingPosts(Pageable pageable) {
        Page<Post> page = postRepository.findTrendingPosts(pageable);
        return convertToPageResponse(page);
    }

    public PageResponse<PostDTO> getFeedPosts(List<Long> followingDoctorIds, Pageable pageable) {
        Page<Post> page = postRepository.findFeedPosts(followingDoctorIds, pageable);
        return convertToPageResponse(page);
    }

    @Transactional
    @CacheEvict(value = "posts", key = "#id")
    public PostDTO updatePost(Long id, CreatePostDTO dto) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (dto.getTitle() != null) post.setTitle(dto.getTitle());
        if (dto.getContent() != null) post.setContent(dto.getContent());
        if (dto.getSpecialty() != null) post.setSpecialty(dto.getSpecialty());
        if (dto.getTags() != null) post.setTags(dto.getTags());
        if (dto.getMediaUrls() != null) post.setMediaUrls(dto.getMediaUrls());
        if (dto.getPaperUrls() != null) post.setPaperUrls(dto.getPaperUrls());

        post = postRepository.save(post);
        return convertToDTO(post);
    }

    @Transactional
    @CacheEvict(value = "posts", key = "#postId")
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        postRepository.delete(post);

        // Publish event
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "POST_DELETED");
        event.put("postId", postId);
        kafkaTemplate.send("post-events", event);
    }

    @Transactional
    public void likePost(Long postId, Long doctorId) {
        if (likeRepository.existsByPostIdAndDoctorId(postId, doctorId)) {
            throw new BadRequestException("Already liked this post");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        Like like = new Like();
        like.setPostId(postId);
        like.setDoctorId(doctorId);
        likeRepository.save(like);

        post.setLikeCount(post.getLikeCount() + 1);
        postRepository.save(post);

        // Publish event
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "POST_LIKED");
        event.put("postId", postId);
        event.put("doctorId", doctorId);
        kafkaTemplate.send("post-events", event);
    }

    @Transactional
    public void unlikePost(Long postId, Long doctorId) {
        Like like = likeRepository.findByPostIdAndDoctorId(postId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Like not found"));

        likeRepository.delete(like);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        postRepository.save(post);
    }

    private PostDTO convertToDTO(Post post) {
        PostDTO dto = new PostDTO();
        dto.setId(post.getId());
        dto.setDoctorId(post.getDoctorId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setPostType(post.getPostType());
        dto.setSpecialty(post.getSpecialty());
        dto.setTags(post.getTags());
        dto.setMediaUrls(post.getMediaUrls());
        dto.setPaperUrls(post.getPaperUrls());
        dto.setPatientAge(post.getPatientAge());
        dto.setPatientGender(post.getPatientGender());
        dto.setDiagnosis(post.getDiagnosis());
        dto.setTreatment(post.getTreatment());
        dto.setOutcome(post.getOutcome());
        dto.setViewCount(post.getViewCount());
        dto.setLikeCount(post.getLikeCount());
        dto.setCommentCount(post.getCommentCount());
        dto.setShareCount(post.getShareCount());
        dto.setAiAnalyzed(post.getAiAnalyzed());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        return dto;
    }

    private PageResponse<PostDTO> convertToPageResponse(Page<Post> page) {
        PageResponse<PostDTO> response = new PageResponse<>();
        response.setContent(page.getContent().stream().map(this::convertToDTO).toList());
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setLast(page.isLast());
        return response;
    }
}
