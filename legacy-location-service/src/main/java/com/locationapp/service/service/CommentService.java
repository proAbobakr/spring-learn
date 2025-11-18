package com.locationapp.service.service;

import com.locationapp.service.dto.CommentRequestDTO;
import com.locationapp.service.dto.CommentResponseDTO;
import com.locationapp.service.event.CommentEvent;
import com.locationapp.service.exception.BadRequestException;
import com.locationapp.service.exception.ResourceNotFoundException;
import com.locationapp.service.model.Comment;
import com.locationapp.service.model.Location;
import com.locationapp.service.model.User;
import com.locationapp.service.repository.CommentRepository;
import com.locationapp.service.repository.LocationRepository;
import com.locationapp.service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Comment Service
 */
@Service
@Slf4j
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private EventPublisher eventPublisher;

    /**
     * Add a comment
     */
    @Transactional
    public CommentResponseDTO addComment(CommentRequestDTO request) {
        User user = getCurrentUser();
        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", request.getLocationId()));

        Comment comment = Comment.builder()
                .content(request.getContent())
                .user(user)
                .location(location)
                .edited(false)
                .likeCount(0)
                .build();

        // Handle reply
        if (request.getParentCommentId() != null) {
            Comment parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", request.getParentCommentId()));

            if (!parentComment.getLocation().getId().equals(location.getId())) {
                throw new BadRequestException("Parent comment belongs to different location");
            }

            comment.setParentComment(parentComment);
        }

        comment = commentRepository.save(comment);

        // Publish event
        CommentEvent event = CommentEvent.builder()
                .eventType(CommentEvent.EventType.CREATED)
                .commentId(comment.getId())
                .locationId(location.getId())
                .locationName(location.getName())
                .userId(user.getId())
                .username(user.getUsername())
                .content(comment.getContent())
                .parentCommentId(request.getParentCommentId())
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishCommentEvent(event);

        log.info("Comment created: {} for location: {} by user: {}",
                comment.getId(), location.getId(), user.getUsername());

        return mapToResponse(comment);
    }

    /**
     * Get comment by ID
     */
    public CommentResponseDTO getCommentById(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));
        return mapToResponse(comment);
    }

    /**
     * Get all comments for a location (top-level only, with replies nested)
     */
    public List<CommentResponseDTO> getCommentsForLocation(Long locationId) {
        List<Comment> topLevelComments = commentRepository
                .findByLocationIdAndParentCommentIsNullOrderByCreatedAtDesc(locationId);

        return topLevelComments.stream()
                .map(this::mapToResponseWithReplies)
                .collect(Collectors.toList());
    }

    /**
     * Get comments with pagination
     */
    public Page<CommentResponseDTO> getCommentsForLocationPaginated(Long locationId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByLocationIdAndParentCommentIsNull(locationId, pageable);
        return comments.map(this::mapToResponseWithReplies);
    }

    /**
     * Get replies for a comment
     */
    public List<CommentResponseDTO> getRepliesForComment(Long commentId) {
        List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(commentId);
        return replies.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update comment
     */
    @Transactional
    public CommentResponseDTO updateComment(Long id, String content) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));

        User currentUser = getCurrentUser();
        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You can only update your own comments");
        }

        comment.setContent(content);
        comment.setEdited(true);
        comment = commentRepository.save(comment);

        // Publish event
        CommentEvent event = CommentEvent.builder()
                .eventType(CommentEvent.EventType.UPDATED)
                .commentId(comment.getId())
                .locationId(comment.getLocation().getId())
                .locationName(comment.getLocation().getName())
                .userId(currentUser.getId())
                .username(currentUser.getUsername())
                .content(comment.getContent())
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishCommentEvent(event);

        return mapToResponse(comment);
    }

    /**
     * Delete comment
     */
    @Transactional
    public void deleteComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));

        User currentUser = getCurrentUser();
        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You can only delete your own comments");
        }

        commentRepository.delete(comment);

        // Publish event
        CommentEvent event = CommentEvent.builder()
                .eventType(CommentEvent.EventType.DELETED)
                .commentId(id)
                .locationId(comment.getLocation().getId())
                .locationName(comment.getLocation().getName())
                .userId(currentUser.getId())
                .username(currentUser.getUsername())
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishCommentEvent(event);

        log.info("Comment deleted: {} by user: {}", id, currentUser.getUsername());
    }

    /**
     * Like/unlike comment
     */
    @Transactional
    public CommentResponseDTO likeComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));

        comment.incrementLikeCount();
        comment = commentRepository.save(comment);

        // Publish event
        User currentUser = getCurrentUser();
        CommentEvent event = CommentEvent.builder()
                .eventType(CommentEvent.EventType.LIKED)
                .commentId(comment.getId())
                .locationId(comment.getLocation().getId())
                .locationName(comment.getLocation().getName())
                .userId(currentUser.getId())
                .username(currentUser.getUsername())
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishCommentEvent(event);

        return mapToResponse(comment);
    }

    private User getCurrentUser() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private CommentResponseDTO mapToResponse(Comment comment) {
        CommentResponseDTO dto = modelMapper.map(comment, CommentResponseDTO.class);
        dto.setLocationId(comment.getLocation().getId());
        dto.setLocationName(comment.getLocation().getName());
        dto.setUserId(comment.getUser().getId());
        dto.setUsername(comment.getUser().getUsername());
        dto.setUserProfileImage(comment.getUser().getProfileImageUrl());
        dto.setIsReply(comment.isReply());
        dto.setParentCommentId(comment.getParentComment() != null ?
                comment.getParentComment().getId() : null);
        return dto;
    }

    private CommentResponseDTO mapToResponseWithReplies(Comment comment) {
        CommentResponseDTO dto = mapToResponse(comment);

        // Load replies
        List<CommentResponseDTO> replies = comment.getReplies().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        dto.setReplies(replies);
        dto.setReplyCount(replies.size());

        return dto;
    }
}
