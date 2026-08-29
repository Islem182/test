package com.dgi.platform.service.impl;

import com.dgi.platform.dto.ChangePasswordRequest;
import com.dgi.platform.dto.UpdateProfileRequest;
import com.dgi.platform.dto.UserProfileDto;
import com.dgi.platform.entity.FiscalNumber;
import com.dgi.platform.entity.Taxpayer;
import com.dgi.platform.entity.User;
import com.dgi.platform.exception.BadRequestException;
import com.dgi.platform.exception.ResourceNotFoundException;
import com.dgi.platform.mapper.UserMapper;
import com.dgi.platform.repository.FiscalNumberRepository;
import com.dgi.platform.repository.TaxpayerRepository;
import com.dgi.platform.repository.UserRepository;
import com.dgi.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final FiscalNumberRepository fiscalNumberRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Taxpayer taxpayer = taxpayerRepository.findByUser(user).orElse(null);
        String fiscalNumStr = null;
        if (taxpayer != null) {
            fiscalNumStr = fiscalNumberRepository.findByTaxpayerId(taxpayer.getId())
                    .map(FiscalNumber::getFiscalNumber)
                    .orElse(null);
        }

        return userMapper.toUserProfileDto(user, taxpayer, fiscalNumStr);
    }

    @Override
    @Transactional
    public UserProfileDto updateProfile(String username, UpdateProfileRequest updateProfileRequest) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (updateProfileRequest.getPhone() != null) {
            user.setPhone(updateProfileRequest.getPhone());
        }
        userRepository.save(user);

        Taxpayer taxpayer = taxpayerRepository.findByUser(user).orElse(null);
        if (taxpayer != null) {
            if (updateProfileRequest.getFirstName() != null) taxpayer.setFirstName(updateProfileRequest.getFirstName());
            if (updateProfileRequest.getLastName() != null) taxpayer.setLastName(updateProfileRequest.getLastName());
            if (updateProfileRequest.getCin() != null) taxpayer.setCin(updateProfileRequest.getCin());
            if (updateProfileRequest.getPassport() != null) taxpayer.setPassport(updateProfileRequest.getPassport());
            if (updateProfileRequest.getBirthDate() != null) taxpayer.setBirthDate(updateProfileRequest.getBirthDate());
            if (updateProfileRequest.getAddress() != null) taxpayer.setAddress(updateProfileRequest.getAddress());
            if (updateProfileRequest.getProfilePicture() != null) taxpayer.setProfilePicture(updateProfileRequest.getProfilePicture());
            taxpayerRepository.save(taxpayer);
        }

        String fiscalNumStr = null;
        if (taxpayer != null) {
            fiscalNumStr = fiscalNumberRepository.findByTaxpayerId(taxpayer.getId())
                    .map(FiscalNumber::getFiscalNumber)
                    .orElse(null);
        }

        return userMapper.toUserProfileDto(user, taxpayer, fiscalNumStr);
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest changePasswordRequest) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password does not match");
        }

        user.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        userRepository.save(user);
    }
}
