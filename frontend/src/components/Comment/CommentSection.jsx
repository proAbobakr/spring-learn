import React, { useState } from 'react';
import { FaThumbsUp, FaReply, FaEdit, FaTrash } from 'react-icons/fa';
import commentService from '../../services/commentService';
import { useAuth } from '../../context/AuthContext';
import { formatTimeAgo } from '../../utils/formatters';
import { toast } from 'react-toastify';
import './CommentSection.css';

const CommentSection = ({ locationId, comments, onCommentAdded }) => {
  const { user, isAuthenticated } = useAuth();
  const [newComment, setNewComment] = useState('');
  const [replyTo, setReplyTo] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmitComment = async (e, parentCommentId = null) => {
    e.preventDefault();
    if (!newComment.trim()) return;

    setSubmitting(true);
    try {
      await commentService.createComment({
        locationId: parseInt(locationId),
        content: newComment,
        parentCommentId: parentCommentId,
      });
      setNewComment('');
      setReplyTo(null);
      toast.success('Comment posted!');
      onCommentAdded();
    } catch (error) {
      toast.error('Failed to post comment');
    } finally {
      setSubmitting(false);
    }
  };

  const handleLike = async (commentId) => {
    if (!isAuthenticated) {
      toast.info('Please login to like comments');
      return;
    }

    try {
      await commentService.likeComment(commentId);
      onCommentAdded();
    } catch (error) {
      toast.error('Failed to like comment');
    }
  };

  const handleDelete = async (commentId) => {
    if (window.confirm('Are you sure you want to delete this comment?')) {
      try {
        await commentService.deleteComment(commentId);
        toast.success('Comment deleted');
        onCommentAdded();
      } catch (error) {
        toast.error('Failed to delete comment');
      }
    }
  };

  const renderComment = (comment, isReply = false) => {
    const isOwner = user && comment.userId === user.id;

    return (
      <div key={comment.id} className={`comment ${isReply ? 'reply' : ''}`}>
        <div className="comment-header">
          <div className="comment-user">
            <div className="user-avatar">
              {comment.userName?.charAt(0).toUpperCase()}
            </div>
            <div>
              <span className="user-name">{comment.userName}</span>
              <span className="comment-date">{formatTimeAgo(comment.createdAt)}</span>
              {comment.edited && <span className="edited-badge">edited</span>}
            </div>
          </div>
          {isOwner && (
            <button
              onClick={() => handleDelete(comment.id)}
              className="btn-icon-delete"
              title="Delete"
            >
              <FaTrash />
            </button>
          )}
        </div>
        <div className="comment-content">
          <p>{comment.content}</p>
        </div>
        <div className="comment-actions">
          <button onClick={() => handleLike(comment.id)} className="btn-icon">
            <FaThumbsUp /> {comment.likeCount || 0}
          </button>
          {isAuthenticated && !isReply && (
            <button onClick={() => setReplyTo(comment.id)} className="btn-icon">
              <FaReply /> Reply
            </button>
          )}
        </div>

        {replyTo === comment.id && (
          <form onSubmit={(e) => handleSubmitComment(e, comment.id)} className="reply-form">
            <textarea
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
              placeholder="Write a reply..."
              rows="3"
              autoFocus
            />
            <div className="form-actions">
              <button type="submit" disabled={submitting} className="btn-submit-small">
                Post Reply
              </button>
              <button type="button" onClick={() => setReplyTo(null)} className="btn-cancel-small">
                Cancel
              </button>
            </div>
          </form>
        )}

        {comment.replies && comment.replies.length > 0 && (
          <div className="replies">
            {comment.replies.map((reply) => renderComment(reply, true))}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="comment-section-container">
      {isAuthenticated ? (
        <form onSubmit={(e) => handleSubmitComment(e)} className="comment-form">
          <textarea
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
            placeholder="Add a comment..."
            rows="4"
          />
          <button type="submit" disabled={submitting} className="btn-submit">
            {submitting ? 'Posting...' : 'Post Comment'}
          </button>
        </form>
      ) : (
        <p className="login-prompt">Please login to post comments</p>
      )}

      <div className="comments-list">
        {comments && comments.length > 0 ? (
          comments.map((comment) => renderComment(comment))
        ) : (
          <p className="no-comments">No comments yet. Be the first to comment!</p>
        )}
      </div>
    </div>
  );
};

export default CommentSection;
