package com.lostfound.web;

import com.lostfound.model.Item;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Public view of an item: no email, no verification answer (the question itself is public). */
public record ItemDto(Long id, String title, String description, String category, String categoryValue,
                      String type, String status, String location, LocalDate eventDate,
                      String reportedBy, Long reporterId, String verificationQuestion,
                      LocalDateTime createdAt) {

    public static ItemDto of(Item i) {
        String q = i.getVerificationQuestion();
        return new ItemDto(i.getId(), i.getTitle(), i.getDescription(), i.getCategory().getLabel(),
                i.getCategory().name(), i.getType().name(), i.getStatus().name(), i.getLocation(),
                i.getEventDate(), i.getReporter().getFullName(), i.getReporter().getId(),
                q == null || q.isBlank() ? null : q, i.getCreatedAt());
    }
}
