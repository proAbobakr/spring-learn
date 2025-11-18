import React, { useState, useEffect } from 'react';
import { FaMapMarkerAlt, FaStar, FaUser } from 'react-icons/fa';
import { useAuth } from '../context/AuthContext';
import locationService from '../services/locationService';
import ratingService from '../services/ratingService';
import LocationCard from '../components/Location/LocationCard';
import { formatDate } from '../utils/formatters';
import { toast } from 'react-toastify';
import './Profile.css';

const Profile = () => {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('locations');
  const [userLocations, setUserLocations] = useState([]);
  const [userRatings, setUserRatings] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (user) {
      fetchUserData();
    }
  }, [user]);

  const fetchUserData = async () => {
    try {
      const [locations, ratings] = await Promise.all([
        locationService.getUserLocations(user.id),
        ratingService.getUserRatings(user.id),
      ]);
      setUserLocations(locations);
      setUserRatings(ratings);
    } catch (error) {
      toast.error('Failed to load profile data');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Loading profile...</p>
      </div>
    );
  }

  return (
    <div className="profile-page">
      <div className="profile-header">
        <div className="profile-avatar">
          {user?.username?.charAt(0).toUpperCase()}
        </div>
        <div className="profile-info">
          <h1>{user?.fullName || user?.username}</h1>
          <p className="username">@{user?.username}</p>
          <p className="email">{user?.email}</p>
          {user?.bio && <p className="bio">{user.bio}</p>}
          <p className="member-since">Member since {formatDate(user?.createdAt)}</p>
        </div>
      </div>

      <div className="profile-tabs">
        <button
          className={`tab ${activeTab === 'locations' ? 'active' : ''}`}
          onClick={() => setActiveTab('locations')}
        >
          <FaMapMarkerAlt /> My Locations ({userLocations.length})
        </button>
        <button
          className={`tab ${activeTab === 'ratings' ? 'active' : ''}`}
          onClick={() => setActiveTab('ratings')}
        >
          <FaStar /> My Reviews ({userRatings.length})
        </button>
      </div>

      <div className="profile-content">
        {activeTab === 'locations' && (
          <div className="locations-grid">
            {userLocations.length > 0 ? (
              userLocations.map((location) => (
                <LocationCard key={location.id} location={location} />
              ))
            ) : (
              <div className="empty-state">
                <FaMapMarkerAlt />
                <p>You haven't added any locations yet</p>
              </div>
            )}
          </div>
        )}

        {activeTab === 'ratings' && (
          <div className="ratings-list">
            {userRatings.length > 0 ? (
              userRatings.map((rating) => (
                <div key={rating.id} className="rating-card">
                  <div className="rating-card-header">
                    <h3>{rating.locationName}</h3>
                    <div className="rating-score">
                      <FaStar className="star-icon" />
                      <span>{rating.score.toFixed(1)}</span>
                    </div>
                  </div>
                  {rating.review && <p className="rating-review">{rating.review}</p>}
                  <p className="rating-date">{formatDate(rating.createdAt)}</p>
                </div>
              ))
            ) : (
              <div className="empty-state">
                <FaStar />
                <p>You haven't written any reviews yet</p>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default Profile;
