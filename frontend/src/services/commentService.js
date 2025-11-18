import api from './api';

const commentService = {
  createComment: async (commentData) => {
    const response = await api.post('/comments', commentData);
    return response.data;
  },

  getCommentById: async (id) => {
    const response = await api.get(`/comments/${id}`);
    return response.data;
  },

  getLocationComments: async (locationId) => {
    const response = await api.get(`/comments/location/${locationId}`);
    return response.data;
  },

  getLocationCommentsPaginated: async (locationId, page = 0, size = 10) => {
    const response = await api.get(`/comments/location/${locationId}/paginated`, {
      params: { page, size },
    });
    return response.data;
  },

  getCommentReplies: async (commentId) => {
    const response = await api.get(`/comments/${commentId}/replies`);
    return response.data;
  },

  updateComment: async (id, content) => {
    const response = await api.put(`/comments/${id}`, { content });
    return response.data;
  },

  deleteComment: async (id) => {
    await api.delete(`/comments/${id}`);
  },

  likeComment: async (id) => {
    const response = await api.post(`/comments/${id}/like`);
    return response.data;
  },
};

export default commentService;
