/**
 * Format a date string into a human-readable format.
 */
export const formatDate = (dateString?: string): string => {
  if (!dateString) return '—';
  try {
    return new Date(dateString).toLocaleDateString('fr-TN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  } catch {
    return dateString;
  }
};

/**
 * Format a date string with time.
 */
export const formatDateTime = (dateString?: string): string => {
  if (!dateString) return '—';
  try {
    return new Date(dateString).toLocaleString('fr-TN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return dateString;
  }
};

/**
 * Format file size in bytes to human-readable format.
 */
export const formatFileSize = (bytes?: number): string => {
  if (!bytes) return '—';
  const units = ['B', 'KB', 'MB', 'GB'];
  let i = 0;
  let size = bytes;
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024;
    i++;
  }
  return `${size.toFixed(1)} ${units[i]}`;
};

/**
 * Truncate a string to a max length and add ellipsis.
 */
export const truncate = (str: string, maxLength = 50): string => {
  if (!str) return '';
  return str.length > maxLength ? `${str.substring(0, maxLength)}...` : str;
};

/**
 * Get initials from a full name.
 */
export const getInitials = (firstName?: string, lastName?: string, username?: string): string => {
  if (firstName && lastName) return `${firstName[0]}${lastName[0]}`.toUpperCase();
  if (firstName) return firstName[0].toUpperCase();
  if (username) return username[0].toUpperCase();
  return 'U';
};

/**
 * Extract error message from an Axios error.
 */
export const getErrorMessage = (error: unknown): string => {
  if (error && typeof error === 'object') {
    const axiosError = error as { response?: { data?: { message?: string } }; message?: string };
    if (axiosError.response?.data?.message) return axiosError.response.data.message;
    if (axiosError.message) return axiosError.message;
  }
  return 'An unexpected error occurred. Please try again.';
};
