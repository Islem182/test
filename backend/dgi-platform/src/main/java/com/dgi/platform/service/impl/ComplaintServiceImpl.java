package com.dgi.platform.service.impl;

import com.dgi.platform.dto.ComplaintRequest;
import com.dgi.platform.dto.ComplaintResponse;
import com.dgi.platform.entity.Complaint;
import com.dgi.platform.entity.ComplaintStatus;
import com.dgi.platform.entity.Taxpayer;
import com.dgi.platform.entity.User;
import com.dgi.platform.exception.BadRequestException;
import com.dgi.platform.exception.ResourceNotFoundException;
import com.dgi.platform.mapper.ComplaintMapper;
import com.dgi.platform.repository.ComplaintRepository;
import com.dgi.platform.repository.TaxpayerRepository;
import com.dgi.platform.repository.UserRepository;
import com.dgi.platform.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final ComplaintMapper complaintMapper;

    @Override
    @Transactional
    public ComplaintResponse createComplaint(String username, ComplaintRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Taxpayer taxpayer = taxpayerRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("User must be a registered taxpayer to submit complaints"));

        Complaint complaint = Complaint.builder()
                .taxpayer(taxpayer)
                .subject(request.getSubject())
                .message(request.getMessage())
                .status(ComplaintStatus.OPEN)
                .build();

        Complaint savedComplaint = complaintRepository.save(complaint);
        return complaintMapper.toResponse(savedComplaint);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getComplaintsByTaxpayer(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Taxpayer taxpayer = taxpayerRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("User must be a registered taxpayer"));

        return complaintRepository.findByTaxpayerId(taxpayer.getId()).stream()
                .map(complaintMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getAllComplaints() {
        return complaintRepository.findAll().stream()
                .map(complaintMapper::toResponse)
                .collect(Collectors.toList());
    }
}
