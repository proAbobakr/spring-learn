import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import locationService from '../services/locationService';
import LocationCard from '../components/Location/LocationCard';
import { LOCATION_CATEGORIES } from '../utils/constants';
import { toast } from 'react-toastify';
import './LocationList.css';

const LocationList = () => {
  const [locations, setLocations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchParams, setSearchParams] = useSearchParams();

  const category = searchParams.get('category') || '';
  const city = searchParams.get('city') || '';
  const searchQuery = searchParams.get('q') || '';

  useEffect(() => {
    fetchLocations();
  }, [category, city, searchQuery]);

  const fetchLocations = async () => {
    setLoading(true);
    try {
      let data;

      if (searchQuery) {
        data = await locationService.searchLocations(searchQuery);
      } else if (category) {
        data = await locationService.getLocationsByCategory(category);
      } else if (city) {
        data = await locationService.getLocationsByCity(city);
      } else {
        data = await locationService.getAllLocations(0, 50);
        data = data.content || data;
      }

      setLocations(Array.isArray(data) ? data : []);
    } catch (error) {
      toast.error('Failed to load locations');
      setLocations([]);
    } finally {
      setLoading(false);
    }
  };

  const handleCategoryChange = (e) => {
    const newCategory = e.target.value;
    if (newCategory) {
      searchParams.set('category', newCategory);
    } else {
      searchParams.delete('category');
    }
    setSearchParams(searchParams);
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Loading locations...</p>
      </div>
    );
  }

  return (
    <div className="location-list-page">
      <div className="page-header">
        <h1>
          {searchQuery
            ? `Search results for "${searchQuery}"`
            : category
            ? `${category} Locations`
            : city
            ? `Locations in ${city}`
            : 'All Locations'}
        </h1>
        <div className="filters">
          <select value={category} onChange={handleCategoryChange}>
            <option value="">All Categories</option>
            {LOCATION_CATEGORIES.map((cat) => (
              <option key={cat} value={cat}>
                {cat.charAt(0) + cat.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        </div>
      </div>

      {locations.length === 0 ? (
        <div className="no-results">
          <p>No locations found</p>
        </div>
      ) : (
        <div className="location-grid">
          {locations.map((location) => (
            <LocationCard key={location.id} location={location} />
          ))}
        </div>
      )}
    </div>
  );
};

export default LocationList;
