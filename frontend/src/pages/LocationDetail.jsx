import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { FaStar, FaMapMarkerAlt, FaEdit, FaTrash } from 'react-icons/fa';
import locationService from '../services/locationService';
import ratingService from '../services/ratingService';
import commentService from '../services/commentService';
import imageService from '../services/imageService';
import RatingForm from '../components/Rating/RatingForm';
import RatingList from '../components/Rating/RatingList';
import CommentSection from '../components/Comment/CommentSection';
import ImageGallery from '../components/Image/ImageGallery';
import { useAuth } from '../context/AuthContext';
import { toast } from 'react-toastify';
import { formatRating, formatCategoryName } from '../utils/formatters';
import './LocationDetail.css';

const LocationDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();
  const [location, setLocation] = useState(null);
  const [ratings, setRatings] = useState([]);
  const [comments, setComments] = useState([]);
  const [images, setImages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showRatingForm, setShowRatingForm] = useState(false);

  useEffect(() => {
    fetchLocationData();
  }, [id]);

  const fetchLocationData = async () => {
    try {
      const [locationData, ratingsData, commentsData, imagesData] = await Promise.all([
        locationService.getLocationById(id),
        ratingService.getLocationRatings(id),
        commentService.getLocationComments(id),
        imageService.getLocationImages(id),
      ]);

      setLocation(locationData);
      setRatings(ratingsData);
      setComments(commentsData);
      setImages(imagesData);
    } catch (error) {
      toast.error('Failed to load location details');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (window.confirm('Are you sure you want to delete this location?')) {
      try {
        await locationService.deleteLocation(id);
        toast.success('Location deleted successfully');
        navigate('/');
      } catch (error) {
        toast.error('Failed to delete location');
      }
    }
  };

  const handleRatingSubmit = async () => {
    setShowRatingForm(false);
    const ratingsData = await ratingService.getLocationRatings(id);
    const locationData = await locationService.getLocationById(id);
    setRatings(ratingsData);
    setLocation(locationData);
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Loading...</p>
      </div>
    );
  }

  if (!location) {
    return <div className="error-container">Location not found</div>;
  }

  const isOwner = user && location.userId === user.id;

  return (
    <div className="location-detail-page">
      <div className="location-header">
        <div className="location-title-section">
          <h1>{location.name}</h1>
          <div className="location-meta">
            <span className="category-tag">
              {formatCategoryName(location.category)}
            </span>
            <span className="address">
              <FaMapMarkerAlt />
              {location.address}, {location.city}, {location.country}
            </span>
          </div>
        </div>
        {isOwner && (
          <div className="location-actions">
            <button
              onClick={() => navigate(`/locations/${id}/edit`)}
              className="btn-edit"
            >
              <FaEdit /> Edit
            </button>
            <button onClick={handleDelete} className="btn-delete">
              <FaTrash /> Delete
            </button>
          </div>
        )}
      </div>

      <div className="location-rating-summary">
        <div className="rating-display">
          <div className="rating-number">{formatRating(location.averageRating)}</div>
          <FaStar className="star-large" />
        </div>
        <div className="rating-info">
          <p className="rating-count">{location.totalRatings} ratings</p>
          <p className="view-count">{location.viewCount} views</p>
        </div>
      </div>

      {images.length > 0 && <ImageGallery images={images} />}

      <div className="location-content">
        <section className="description-section">
          <h2>About</h2>
          <p>{location.description}</p>
        </section>

        <section className="rating-section">
          <div className="section-title">
            <h2>Ratings & Reviews</h2>
            {isAuthenticated && !showRatingForm && (
              <button
                onClick={() => setShowRatingForm(true)}
                className="btn-primary"
              >
                Write a Review
              </button>
            )}
          </div>

          {showRatingForm && (
            <RatingForm
              locationId={id}
              onSuccess={handleRatingSubmit}
              onCancel={() => setShowRatingForm(false)}
            />
          )}

          <RatingList ratings={ratings} />
        </section>

        <section className="comment-section">
          <h2>Comments</h2>
          <CommentSection
            locationId={id}
            comments={comments}
            onCommentAdded={fetchLocationData}
          />
        </section>
      </div>
    </div>
  );
};

export default LocationDetail;
