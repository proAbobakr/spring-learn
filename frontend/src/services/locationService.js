import api from './api';

const locationService = {
  getAllLocations: async (page = 0, size = 20) => {
    const response = await api.get('/locations', { params: { page, size } });
    return response.data;
  },

  getLocationById: async (id) => {
    const response = await api.get(`/locations/${id}`);
    return response.data;
  },

  createLocation: async (locationData) => {
    const response = await api.post('/locations', locationData);
    return response.data;
  },

  updateLocation: async (id, locationData) => {
    const response = await api.put(`/locations/${id}`, locationData);
    return response.data;
  },

  deleteLocation: async (id) => {
    await api.delete(`/locations/${id}`);
  },

  searchLocations: async (keyword) => {
    const response = await api.get('/locations/search', { params: { keyword } });
    return response.data;
  },

  getNearbyLocations: async (latitude, longitude, radiusKm = 10) => {
    const response = await api.get('/locations/nearby', {
      params: { latitude, longitude, radiusKm },
    });
    return response.data;
  },

  getTopRatedLocations: async (limit = 10) => {
    const response = await api.get('/locations/top-rated', { params: { limit } });
    return response.data;
  },

  getTrendingLocations: async (limit = 10) => {
    const response = await api.get('/locations/trending', { params: { limit } });
    return response.data;
  },

  getLocationsByCity: async (city) => {
    const response = await api.get('/locations/by-city', { params: { city } });
    return response.data;
  },

  getLocationsByCategory: async (category) => {
    const response = await api.get('/locations/by-category', { params: { category } });
    return response.data;
  },

  getUserLocations: async (userId) => {
    const response = await api.get(`/locations/user/${userId}`);
    return response.data;
  },
};

export default locationService;
