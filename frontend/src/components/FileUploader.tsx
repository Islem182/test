import React, { useState } from 'react';
import {
  Box,
  Button,
  Typography,
  LinearProgress,
  Paper,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  IconButton,
  useTheme,
} from '@mui/material';
import {
  CloudUpload as UploadIcon,
  Delete as DeleteIcon,
  CheckCircle as SuccessIcon,
} from '@mui/icons-material';
import { DocumentType, DocumentItem } from '../types';

interface FileUploaderProps {
  onFileUpload: (document: DocumentItem) => void;
  allowedTypes?: DocumentType[];
}

export const FileUploader: React.FC<FileUploaderProps> = ({
  onFileUpload,
  allowedTypes = ['CIN_COPY', 'PASSPORT_COPY', 'PATENTE', 'COMMERCIAL_REGISTER', 'TAX_RETURN', 'OTHER'],
}) => {
  const theme = useTheme();
  const [selectedType, setSelectedType] = useState<DocumentType>('CIN_COPY');
  const [uploading, setUploading] = useState<boolean>(false);
  const [progress, setProgress] = useState<number>(0);
  const [uploadedFile, setUploadedFile] = useState<DocumentItem | null>(null);

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setUploading(true);
    setProgress(0);

    const interval = setInterval(() => {
      setProgress((prev) => {
        if (prev >= 100) {
          clearInterval(interval);
          setUploading(false);

          const doc: DocumentItem = {
            type: selectedType,
            filePath: `/uploads/${Date.now()}_${file.name}`,
            fileName: file.name,
            fileSize: (file.size / 1024 / 1024).toFixed(2) + ' MB',
            uploadDate: new Date().toISOString(),
            verificationStatus: 'PENDING',
          };

          setUploadedFile(doc);
          onFileUpload(doc);
          return 100;
        }
        return prev + 25;
      });
    }, 200);
  };

  const handleRemove = () => {
    setUploadedFile(null);
    setProgress(0);
  };

  return (
    <Paper
      variant="outlined"
      sx={{
        p: 3,
        borderRadius: 3,
        borderStyle: 'dashed',
        borderWidth: 2,
        borderColor: uploading ? theme.palette.primary.main : theme.palette.divider,
        backgroundColor: theme.palette.background.default,
      }}
    >
      {!uploadedFile ? (
        <Box sx={{ textAlign: 'center' }}>
          <FormControl size="small" sx={{ mb: 2, minWidth: 220 }}>
            <InputLabel>Select Document Type</InputLabel>
            <Select
              value={selectedType}
              label="Select Document Type"
              onChange={(e) => setSelectedType(e.target.value as DocumentType)}
            >
              {allowedTypes.map((type) => (
                <MenuItem key={type} value={type}>
                  {type.replace('_', ' ')}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <Box sx={{ mb: 2 }}>
            <UploadIcon sx={{ fontSize: 48, color: theme.palette.primary.main }} />
            <Typography variant="subtitle1" sx={{ fontWeight: 600, mt: 1 }}>
              Drag & Drop or Browse Document
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Supports PDF, PNG, JPG (Max 10MB)
            </Typography>
          </Box>

          <Button variant="contained" component="label" disabled={uploading}>
            Choose File
            <input type="file" hidden accept=".pdf,.png,.jpg,.jpeg" onChange={handleFileSelect} />
          </Button>

          {uploading && (
            <Box sx={{ width: '100%', mt: 3 }}>
              <LinearProgress variant="determinate" value={progress} />
              <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
                Uploading... {progress}%
              </Typography>
            </Box>
          )}
        </Box>
      ) : (
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <SuccessIcon color="success" fontSize="large" />
            <Box>
              <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                {uploadedFile.fileName}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Type: {uploadedFile.type} • {uploadedFile.fileSize}
              </Typography>
            </Box>
          </Box>
          <IconButton color="error" onClick={handleRemove}>
            <DeleteIcon />
          </IconButton>
        </Box>
      )}
    </Paper>
  );
};
