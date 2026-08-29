import React from 'react';
import { Chip } from '@mui/material';

type ChipSize = 'small' | 'medium';

interface StatusBadgeProps {
  status: string;
  size?: ChipSize;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, size = 'medium' }) => {
  let color: 'default' | 'success' | 'warning' | 'error' | 'info' = 'default';
  let label = status;

  switch (status?.toUpperCase()) {
    case 'APPROVED':
    case 'VERIFIED':
    case 'RESOLVED':
      color = 'success';
      break;
    case 'PENDING':
    case 'IN_PROGRESS':
    case 'IN_REVIEW':
    case 'OPEN':
      color = 'warning';
      break;
    case 'REJECTED':
    case 'CLOSED':
      color = 'error';
      break;
    default:
      color = 'info';
  }

  return (
    <Chip
      label={label}
      color={color}
      size={size}
      sx={{
        fontWeight: 600,
        fontSize: size === 'small' ? '0.75rem' : '0.8125rem',
        borderRadius: '6px',
      }}
    />
  );
};
