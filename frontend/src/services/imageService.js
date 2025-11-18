import api from './api';

const imageService = {
  uploadImage: async (file, locationId, caption = '') => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('locationId', locationId);
    if (caption) {
      formData.append('caption', caption);
    }

    const response = await api.post('/images/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  getImageById: async (id) => {
    const response = await api.get(`/images/${id}`);
    return response.data;
  },

  getLocationImages: async (locationId) => {
    const response = await api.get(`/images/location/${locationId}`);
    return response.data;
  },

  getImageUrl: (filename) => {
    return `${api.defaults.baseURL}/images/view/${filename}`;
  },

  updateImage: async (id, updateData) => {
    const response = await api.put(`/images/${id}`, updateData);
    return response.data;
  },

  deleteImage: async (id) => {
    await api.delete(`/images/${id}`);
  },
};

export default imageService;
