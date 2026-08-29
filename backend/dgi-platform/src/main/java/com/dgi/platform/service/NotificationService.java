package com.dgi.platform.service;

import com.dgi.platform.entity.Notification;
import com.dgi.platform.entity.User;

import java.util.List;

public interface NotificationService {
    Notification sendNotification(User user, String title, String message);
    List<Notification> getUserNotifications(Long userId);
    void markAsRead(Long notificationId);
}
