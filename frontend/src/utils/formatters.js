import { format, formatDistanceToNow } from 'date-fns';

export const formatDate = (dateString) => {
  if (!dateString) return '';
  return format(new Date(dateString), 'MMM dd, yyyy');
};

export const formatDateTime = (dateString) => {
  if (!dateString) return '';
  return format(new Date(dateString), 'MMM dd, yyyy HH:mm');
};

export const formatTimeAgo = (dateString) => {
  if (!dateString) return '';
  return formatDistanceToNow(new Date(dateString), { addSuffix: true });
};

export const formatRating = (rating) => {
  if (!rating) return '0.0';
  return Number(rating).toFixed(1);
};

export const formatCategoryName = (category) => {
  if (!category) return '';
  return category.charAt(0) + category.slice(1).toLowerCase();
};
