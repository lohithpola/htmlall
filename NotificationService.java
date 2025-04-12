package com.socialmedia.service;

import com.socialmedia.entity.Comment;
import com.socialmedia.entity.Notification;
import com.socialmedia.entity.Post;
import com.socialmedia.entity.User;
import com.socialmedia.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public Page<Notification> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional
    public void createLikeNotification(User actor, Post post) {
        if (!actor.getId().equals(post.getUser().getId())) {
            Notification notification = new Notification();
            notification.setUser(post.getUser());
            notification.setActor(actor);
            notification.setType(Notification.NotificationType.LIKE);
            notification.setPost(post);
            
            notification = notificationRepository.save(notification);
            sendNotification(notification);
        }
    }

    @Transactional
    public void createCommentNotification(User actor, Post post) {
        if (!actor.getId().equals(post.getUser().getId())) {
            Notification notification = new Notification();
            notification.setUser(post.getUser());
            notification.setActor(actor);
            notification.setType(Notification.NotificationType.COMMENT);
            notification.setPost(post);
            
            notification = notificationRepository.save(notification);
            sendNotification(notification);
        }
    }

    @Transactional
    public void createFollowNotification(User actor, User user) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setActor(actor);
        notification.setType(Notification.NotificationType.FOLLOW);
        
        notification = notificationRepository.save(notification);
        sendNotification(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    private void sendNotification(Notification notification) {
        messagingTemplate.convertAndSendToUser(
            notification.getUser().getUsername(),
            "/queue/notifications",
            notification
        );
    }
} 