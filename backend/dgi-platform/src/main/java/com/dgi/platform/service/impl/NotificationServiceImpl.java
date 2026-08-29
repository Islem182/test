package com.dgi.platform.service.impl;

import com.dgi.platform.entity.Notification;
import com.dgi.platform.entity.User;
import com.dgi.platform.exception.ResourceNotFoundException;
import com.dgi.platform.repository.NotificationRepository;
import com.dgi.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public Notification sendNotification(User user, String title, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .readStatus(false)
                .build();
        return notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        notification.setReadStatus(true);
        notificationRepository.save(notification);
    }
}
