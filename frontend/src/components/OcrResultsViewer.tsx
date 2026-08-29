import React from 'react';
import {
  Card,
  CardContent,
  Typography,
  Box,
  Divider,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Alert,
} from '@mui/material';
import { CheckCircle as MatchIcon, Error as MismatchIcon } from '@mui/icons-material';
import { OcrResult } from '../types';

interface OcrResultsViewerProps {
  ocrResult?: OcrResult;
  userEnteredData?: {
    cin?: string;
    fullName?: string;
    birthDate?: string;
    address?: string;
  };
}

export const OcrResultsViewer: React.FC<OcrResultsViewerProps> = ({
  ocrResult = {
    cinNumber: '08923415',
    fullName: 'Mohamed Ben Ali',
    birthDate: '1988-04-12',
    address: 'Avenue Habib Bourguiba, Tunis',
    confidenceScore: 0.96,
    documentType: 'CIN_COPY',
  },
  userEnteredData = {
    cin: '08923415',
    fullName: 'Mohamed Ben Ali',
    birthDate: '1988-04-12',
    address: 'Avenue Habib Bourguiba, Tunis',
  },
}) => {
  const fields = [
    { label: 'CIN / Document ID', ocr: ocrResult.cinNumber, user: userEnteredData.cin },
    { label: 'Full Name', ocr: ocrResult.fullName, user: userEnteredData.fullName },
    { label: 'Birth Date', ocr: ocrResult.birthDate, user: userEnteredData.birthDate },
    { label: 'Domiciliation Address', ocr: ocrResult.address, user: userEnteredData.address },
  ];

  const allMatched = fields.every(
    (f) => !f.ocr || !f.user || f.ocr.toLowerCase() === f.user.toLowerCase()
  );

  return (
    <Card variant="outlined" sx={{ borderRadius: 3 }}>
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Box>
            <Typography variant="h6" sx={{ fontWeight: 700 }}>
              AI OCR Verification Engine
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Document Type: {ocrResult.documentType} • Accuracy Confidence: {(ocrResult.confidenceScore * 100).toFixed(1)}%
            </Typography>
          </Box>
          <Chip
            label={allMatched ? 'OCR MATCH PASSED' : 'DISCREPANCY DETECTED'}
            color={allMatched ? 'success' : 'error'}
            sx={{ fontWeight: 700 }}
          />
        </Box>

        <Divider sx={{ mb: 2 }} />

        <TableContainer component={Paper} elevation={0} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell sx={{ fontWeight: 700 }}>Field Name</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>OCR Extracted Data</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>User Input Data</TableCell>
                <TableCell sx={{ fontWeight: 700, textAlign: 'center' }}>Match Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {fields.map((f, i) => {
                const match = !f.ocr || !f.user || f.ocr.toLowerCase() === f.user.toLowerCase();
                return (
                  <TableRow key={i}>
                    <TableCell sx={{ fontWeight: 600 }}>{f.label}</TableCell>
                    <TableCell>{f.ocr || 'N/A'}</TableCell>
                    <TableCell>{f.user || 'N/A'}</TableCell>
                    <TableCell sx={{ textAlign: 'center' }}>
                      {match ? (
                        <MatchIcon color="success" fontSize="small" />
                      ) : (
                        <MismatchIcon color="error" fontSize="small" />
                      )}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>

        {!allMatched && (
          <Alert severity="warning" sx={{ mt: 2, borderRadius: 2 }}>
            A mismatch was detected between the document OCR extract and user input. Tax agent manual review will verify original file upload.
          </Alert>
        )}
      </CardContent>
    </Card>
  );
};
