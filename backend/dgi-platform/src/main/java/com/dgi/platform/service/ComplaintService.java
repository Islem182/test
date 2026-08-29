package com.dgi.platform.service;

import com.dgi.platform.dto.ComplaintRequest;
import com.dgi.platform.dto.ComplaintResponse;

import java.util.List;

public interface ComplaintService {
    ComplaintResponse createComplaint(String username, ComplaintRequest request);
    List<ComplaintResponse> getComplaintsByTaxpayer(String username);
    List<ComplaintResponse> getAllComplaints();
}
