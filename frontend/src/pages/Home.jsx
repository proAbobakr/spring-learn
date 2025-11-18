import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { FaStar, FaTrendingUp, FaMapMarkerAlt } from 'react-icons/fa';
import locationService from '../services/locationService';
import LocationCard from '../components/Location/LocationCard';
import { toast } from 'react-toastify';
import useGeolocation from '../hooks/useGeolocation';
import './Home.css';

const Home = () => {
  const [topRated, setTopRated] = useState([]);
  const [trending, setTrending] = useState([]);
  const [nearby, setNearby] = useState([]);
  const [loading, setLoading] = useState(true);
  const { location, getLocation } = useGeolocation();

  useEffect(() => {
    fetchHomeData();
  }, []);

  useEffect(() => {
    if (location) {
      fetchNearbyLocations();
    }
  }, [location]);

  const fetchHomeData = async () => {
    try {
      const [topRatedData, trendingData] = await Promise.all([
        locationService.getTopRatedLocations(6),
        locationService.getTrendingLocations(6),
      ]);
      setTopRated(topRatedData);
      setTrending(trendingData);
    } catch (error) {
      toast.error('Failed to load locations');
    } finally {
      setLoading(false);
    }
  };

  const fetchNearbyLocations = async () => {
    try {
      const data = await locationService.getNearbyLocations(
        location.latitude,
        location.longitude,
        10
      );
      setNearby(data.slice(0, 6));
    } catch (error) {
      console.error('Failed to load nearby locations', error);
    }
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Loading...</p>
      </div>
    );
  }

  return (
    <div className="home-page">
      <section className="hero">
        <div className="hero-content">
          <h1>Discover Amazing Places</h1>
          <p>Find and rate the best locations in your area</p>
          <div className="hero-actions">
            <Link to="/locations" className="btn-hero-primary">
              Explore Locations
            </Link>
            <button onClick={getLocation} className="btn-hero-secondary">
              <FaMapMarkerAlt /> Find Nearby
            </button>
          </div>
        </div>
      </section>

      {nearby.length > 0 && (
        <section className="location-section">
          <div className="section-header">
            <h2>
              <FaMapMarkerAlt /> Nearby Locations
            </h2>
            <Link to="/locations?nearby=true">View all</Link>
          </div>
          <div className="location-grid">
            {nearby.map((location) => (
              <LocationCard key={location.id} location={location} />
            ))}
          </div>
        </section>
      )}

      <section className="location-section">
        <div className="section-header">
          <h2>
            <FaStar /> Top Rated
          </h2>
          <Link to="/locations?sort=rating">View all</Link>
        </div>
        <div className="location-grid">
          {topRated.map((location) => (
            <LocationCard key={location.id} location={location} />
          ))}
        </div>
      </section>

      <section className="location-section">
        <div className="section-header">
          <h2>
            <FaTrendingUp /> Trending Now
          </h2>
          <Link to="/locations?sort=trending">View all</Link>
        </div>
        <div className="location-grid">
          {trending.map((location) => (
            <LocationCard key={location.id} location={location} />
          ))}
        </div>
      </section>
    </div>
  );
};

export default Home;
