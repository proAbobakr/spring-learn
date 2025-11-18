import React, { useState } from 'react';
import { FaStar } from 'react-icons/fa';
import ratingService from '../../services/ratingService';
import { toast } from 'react-toastify';
import './RatingForm.css';

const RatingForm = ({ locationId, onSuccess, onCancel }) => {
  const [score, setScore] = useState(0);
  const [hoverScore, setHoverScore] = useState(0);
  const [review, setReview] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (score === 0) {
      toast.error('Please select a rating');
      return;
    }

    setSubmitting(true);
    try {
      await ratingService.createRating({
        locationId: parseInt(locationId),
        score: score,
        review: review,
      });
      toast.success('Rating submitted successfully!');
      onSuccess();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to submit rating');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="rating-form">
      <h3>Write Your Review</h3>
      <form onSubmit={handleSubmit}>
        <div className="star-rating">
          <label>Your Rating:</label>
          <div className="stars">
            {[1, 2, 3, 4, 5].map((star) => (
              <FaStar
                key={star}
                className={`star ${star <= (hoverScore || score) ? 'active' : ''}`}
                onClick={() => setScore(star)}
                onMouseEnter={() => setHoverScore(star)}
                onMouseLeave={() => setHoverScore(0)}
              />
            ))}
          </div>
        </div>

        <div className="form-group">
          <label htmlFor="review">Your Review:</label>
          <textarea
            id="review"
            value={review}
            onChange={(e) => setReview(e.target.value)}
            placeholder="Share your experience..."
            rows="5"
          />
        </div>

        <div className="form-actions">
          <button type="submit" className="btn-submit" disabled={submitting}>
            {submitting ? 'Submitting...' : 'Submit Review'}
          </button>
          <button type="button" onClick={onCancel} className="btn-cancel">
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
};

export default RatingForm;
