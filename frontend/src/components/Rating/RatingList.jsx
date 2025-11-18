import React from 'react';
import { FaStar } from 'react-icons/fa';
import { formatTimeAgo, formatRating } from '../../utils/formatters';
import './RatingList.css';

const RatingList = ({ ratings }) => {
  if (!ratings || ratings.length === 0) {
    return <p className="no-ratings">No ratings yet. Be the first to review!</p>;
  }

  return (
    <div className="rating-list">
      {ratings.map((rating) => (
        <div key={rating.id} className="rating-item">
          <div className="rating-header">
            <div className="rating-user">
              <div className="user-avatar">
                {rating.userName?.charAt(0).toUpperCase()}
              </div>
              <div className="user-info">
                <span className="user-name">{rating.userName}</span>
                <span className="rating-date">{formatTimeAgo(rating.createdAt)}</span>
              </div>
            </div>
            <div className="rating-score">
              <FaStar className="star-icon" />
              <span>{formatRating(rating.score)}</span>
            </div>
          </div>
          {rating.review && (
            <div className="rating-review">
              <p>{rating.review}</p>
            </div>
          )}
        </div>
      ))}
    </div>
  );
};

export default RatingList;
