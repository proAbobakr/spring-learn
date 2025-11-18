import React from 'react';
import { Link } from 'react-router-dom';
import { FaStar, FaMapMarkerAlt } from 'react-icons/fa';
import { formatRating, formatCategoryName } from '../../utils/formatters';
import './LocationCard.css';

const LocationCard = ({ location }) => {
  const defaultImage = 'https://via.placeholder.com/400x250?text=No+Image';

  return (
    <Link to={`/locations/${location.id}`} className="location-card">
      <div className="location-image">
        <img
          src={location.primaryImageUrl || defaultImage}
          alt={location.name}
          onError={(e) => {
            e.target.src = defaultImage;
          }}
        />
        <span className="category-badge">
          {formatCategoryName(location.category)}
        </span>
      </div>
      <div className="location-info">
        <h3>{location.name}</h3>
        <p className="location-address">
          <FaMapMarkerAlt /> {location.city}, {location.country}
        </p>
        <p className="location-description">
          {location.description?.substring(0, 100)}
          {location.description?.length > 100 ? '...' : ''}
        </p>
        <div className="location-footer">
          <div className="rating">
            <FaStar className="star-icon" />
            <span className="rating-value">
              {formatRating(location.averageRating)}
            </span>
            <span className="rating-count">({location.totalRatings})</span>
          </div>
        </div>
      </div>
    </Link>
  );
};

export default LocationCard;
