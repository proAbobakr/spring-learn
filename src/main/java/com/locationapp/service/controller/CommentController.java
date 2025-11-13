package com.locationapp.service.controller;

import com.locationapp.service.dto.CommentRequestDTO;
import com.locationapp.service.dto.CommentResponseDTO;
import com.locationapp.service.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Comment Controller
 *
 * REST endpoints for comments and replies.
 */
@RestController
@RequestMapping("/api/comments")
@Tag(name = "Comments", description = "Comment and reply endpoints")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Add a comment or reply", description = "Creates a new comment or reply to existing comment")
    public ResponseEntity<CommentResponseDTO> addComment(@Valid @RequestBody CommentRequestDTO request) {
        CommentResponseDTO response = commentService.addComment(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get comment by ID", description = "Returns comment details")
    public ResponseEntity<CommentResponseDTO> getComment(@PathVariable Long id) {
        CommentResponseDTO response = commentService.getCommentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/location/{locationId}")
    @Operation(summary = "Get comments for location", description = "Returns all top-level comments with nested replies")
    public ResponseEntity<List<CommentResponseDTO>> getCommentsForLocation(@PathVariable Long locationId) {
        List<CommentResponseDTO> comments = commentService.getCommentsForLocation(locationId);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/location/{locationId}/paginated")
    @Operation(summary = "Get comments with pagination", description = "Returns paginated comments")
    public ResponseEntity<Page<CommentResponseDTO>> getCommentsPaginated(
            @PathVariable Long locationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<CommentResponseDTO> comments = commentService.getCommentsForLocationPaginated(locationId, page, size);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/{commentId}/replies")
    @Operation(summary = "Get replies for comment", description = "Returns all replies to a specific comment")
    public ResponseEntity<List<CommentResponseDTO>> getReplies(@PathVariable Long commentId) {
        List<CommentResponseDTO> replies = commentService.getRepliesForComment(commentId);
        return ResponseEntity.ok(replies);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Update comment", description = "Updates comment content (owner only)")
    public ResponseEntity<CommentResponseDTO> updateComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String content = body.get("content");
        CommentResponseDTO response = commentService.updateComment(id, content);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Delete comment", description = "Deletes a comment (owner only)")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/like")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Like a comment", description = "Increments like count for a comment")
    public ResponseEntity<CommentResponseDTO> likeComment(@PathVariable Long id) {
        CommentResponseDTO response = commentService.likeComment(id);
        return ResponseEntity.ok(response);
    }
}
