import api from './api';

const ratingService = {
  createRating: async (ratingData) => {
    const response = await api.post('/ratings', ratingData);
    return response.data;
  },

  getRatingById: async (id) => {
    const response = await api.get(`/ratings/${id}`);
    return response.data;
  },

  getLocationRatings: async (locationId) => {
    const response = await api.get(`/ratings/location/${locationId}`);
    return response.data;
  },

  getRatingDistribution: async (locationId) => {
    const response = await api.get(`/ratings/location/${locationId}/distribution`);
    return response.data;
  },

  getUserRatings: async (userId) => {
    const response = await api.get(`/ratings/user/${userId}`);
    return response.data;
  },

  deleteRating: async (id) => {
    await api.delete(`/ratings/${id}`);
  },
};

export default ratingService;
