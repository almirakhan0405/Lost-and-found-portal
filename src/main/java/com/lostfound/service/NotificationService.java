package com.lostfound.service;

import com.lostfound.model.Notification;
import com.lostfound.model.User;
import com.lostfound.repo.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    public void notify(User user, String message) {
        Notification n = new Notification();
        n.setUser(user);
        n.setMessage(message);
        repo.save(n);
    }

    @Transactional(readOnly = true)
    public List<Notification> forUser(User user) {
        return repo.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return repo.countByUserAndSeenFalse(user);
    }

    public void markAllRead(User user) {
        List<Notification> list = repo.findByUserOrderByCreatedAtDesc(user);
        list.forEach(n -> n.setSeen(true));
        repo.saveAll(list);
    }
}
