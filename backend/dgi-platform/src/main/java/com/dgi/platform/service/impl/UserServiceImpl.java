package com.dgi.platform.service.impl;

import com.dgi.platform.dto.ChangePasswordRequest;
import com.dgi.platform.dto.UpdateProfileRequest;
import com.dgi.platform.dto.UserProfileDto;
import com.dgi.platform.entity.User;
import com.dgi.platform.exception.BadRequestException;
import com.dgi.platform.exception.ResourceNotFoundException;
import com.dgi.platform.mapper.UserMapper;
import com.dgi.platform.repository.FiscalNumberRepository;
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
    private final FiscalNumberRepository fiscalNumberRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        String fiscalNumber = fiscalNumberRepository.findByUserId(user.getId())
                .map(fn -> fn.getFiscalNumber()).orElse(null);
        return userMapper.toUserProfileDto(user, fiscalNumber);
    }

    @Override
    @Transactional
    public UserProfileDto updateProfile(String username, UpdateProfileRequest req) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (req.getPhone()          != null) user.setPhone(req.getPhone());
        if (req.getFirstName()      != null) user.setFirstName(req.getFirstName());
        if (req.getLastName()       != null) user.setLastName(req.getLastName());
        if (req.getCin()            != null) user.setCin(req.getCin().isBlank() ? null : req.getCin().trim());
        if (req.getPassport()       != null) user.setPassport(req.getPassport().isBlank() ? null : req.getPassport().trim());
        if (req.getBirthDate()      != null) user.setBirthDate(req.getBirthDate());
        if (req.getAddress()        != null) user.setAddress(req.getAddress());
        if (req.getProfilePicture() != null) user.setProfilePicture(req.getProfilePicture());

        User saved = userRepository.save(user);
        String fiscalNumber = fiscalNumberRepository.findByUserId(saved.getId())
                .map(fn -> fn.getFiscalNumber()).orElse(null);
        return userMapper.toUserProfileDto(saved, fiscalNumber);
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest req) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword()))
            throw new BadRequestException("Current password does not match");
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }
}
