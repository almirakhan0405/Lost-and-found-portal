package com.lostfound.web;

import com.lostfound.model.*;
import com.lostfound.service.AuthService;
import com.lostfound.service.ClaimService;
import com.lostfound.service.ItemService;
import com.lostfound.service.MatchingService;
import com.lostfound.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/** Actions for signed-in users: account, reporting, claims and notifications (session cookie login). */
@RestController
@RequestMapping("/api")
public class WebApiController {

    public record RegisterRequest(String username, String fullName, String email, String password) { }
    public record LoginRequest(String username, String password) { }
    public record ReportRequest(String type, String title, String category, String location, String eventDate,
                                String description, String verificationQuestion, String verificationAnswer) { }
    public record ClaimRequest(String message, String answer) { }
    public record UserDto(Long id, String username, String fullName, String role) { }
    public record ClaimDto(Long id, Long itemId, String itemTitle, String itemType, String claimant,
                           String claimantEmail, String message, String answerGiven, String expectedAnswer,
                           String reporter, String status, LocalDateTime createdAt) { }
    public record NotificationDto(Long id, String message, boolean seen, LocalDateTime createdAt) { }

    private final AuthService auth;
    private final ItemService items;
    private final ClaimService claims;
    private final NotificationService notifications;
    private final SessionUser session;

    public WebApiController(AuthService auth, ItemService items, ClaimService claims,
                            NotificationService notifications, SessionUser session) {
        this.auth = auth;
        this.items = items;
        this.claims = claims;
        this.notifications = notifications;
        this.session = session;
    }

    private static UserDto dto(User u) {
        return new UserDto(u.getId(), u.getUsername(), u.getFullName(), u.getRole().name());
    }

    // ---------------------------------------------------------------- account

    @PostMapping("/auth/register")
    public UserDto register(@RequestBody RegisterRequest r, HttpSession http) {
        auth.register(r.username(), r.fullName(), r.email(), r.password());
        User u = auth.login(r.username(), r.password());
        session.signIn(http, u);
        return dto(u);
    }

    @PostMapping("/auth/login")
    public UserDto login(@RequestBody LoginRequest r, HttpSession http) {
        User u = auth.login(r.username(), r.password());
        session.signIn(http, u);
        return dto(u);
    }

    @PostMapping("/auth/logout")
    public Map<String, Boolean> logout(HttpSession http) {
        http.invalidate();
        return Map.of("ok", true);
    }

    @GetMapping("/auth/me")
    public Map<String, Object> me(HttpSession http) {
        User u = session.current(http);
        Map<String, Object> m = new HashMap<>();
        m.put("user", u == null ? null : dto(u));
        return m;
    }

    @GetMapping("/meta/categories")
    public List<Map<String, String>> categories() {
        List<Map<String, String>> out = new ArrayList<>();
        for (Category c : Category.values()) out.add(Map.of("value", c.name(), "label", c.getLabel()));
        return out;
    }

    // ------------------------------------------------------------------ items

    @PostMapping("/items")
    public Map<String, Object> report(@RequestBody ReportRequest r, HttpSession http) {
        User u = session.require(http);
        Item i = new Item();
        i.setReporter(u);
        i.setType(parse(ItemType.class, r.type(), "Choose Lost or Found."));
        i.setCategory(parse(Category.class, r.category(), "Choose a category."));
        i.setTitle(r.title());
        i.setLocation(r.location());
        i.setDescription(r.description() == null ? "" : r.description().trim());
        i.setVerificationQuestion(r.verificationQuestion() == null ? null : r.verificationQuestion().trim());
        i.setVerificationAnswer(r.verificationAnswer() == null ? null : r.verificationAnswer().trim());
        try {
            i.setEventDate(r.eventDate() == null || r.eventDate().isBlank() ? null : LocalDate.parse(r.eventDate()));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date must look like 2026-10-17.");
        }
        List<MatchingService.Match> found = items.report(i);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("item", ItemDto.of(i));
        out.put("matches", found.stream()
                .map(m -> new ApiController.MatchDto(ItemDto.of(m.item()), m.score(), m.reasons())).toList());
        return out;
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, String message) {
        try {
            return Enum.valueOf(type, value == null ? "" : value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(message);
        }
    }

    @GetMapping("/my/items")
    public List<ItemDto> myItems(HttpSession http) {
        return items.forReporter(session.require(http)).stream().map(ItemDto::of).toList();
    }

    @DeleteMapping("/items/{id}")
    public Map<String, Boolean> delete(@PathVariable Long id, HttpSession http) {
        items.delete(id, session.require(http));
        return Map.of("ok", true);
    }

    @PostMapping("/items/{id}/status/{status}")
    public Map<String, Boolean> setStatus(@PathVariable Long id, @PathVariable String status, HttpSession http) {
        items.setStatus(id, parse(ItemStatus.class, status, "Unknown status."), session.require(http));
        return Map.of("ok", true);
    }

    // ----------------------------------------------------------------- claims

    @PostMapping("/items/{id}/claim")
    public Map<String, Boolean> claim(@PathVariable Long id, @RequestBody ClaimRequest r, HttpSession http) {
        claims.submit(id, session.require(http), r.message(), r.answer());
        return Map.of("ok", true);
    }

    @GetMapping("/claims/incoming")
    public List<ClaimDto> incoming(HttpSession http) {
        return claims.onMyItems(session.require(http)).stream().map(c -> toDto(c, true)).toList();
    }

    @GetMapping("/claims/mine")
    public List<ClaimDto> mine(HttpSession http) {
        return claims.byClaimant(session.require(http)).stream().map(c -> toDto(c, false)).toList();
    }

    @PostMapping("/claims/{id}/approve")
    public Map<String, Boolean> approve(@PathVariable Long id, HttpSession http) {
        claims.approve(id, session.require(http));
        return Map.of("ok", true);
    }

    @PostMapping("/claims/{id}/reject")
    public Map<String, Boolean> reject(@PathVariable Long id, HttpSession http) {
        claims.reject(id, session.require(http));
        return Map.of("ok", true);
    }

    /** The reporter sees the claimant's email and the expected answer; the claimant never sees the answer. */
    private static ClaimDto toDto(Claim c, boolean reporterView) {
        Item i = c.getItem();
        return new ClaimDto(c.getId(), i.getId(), i.getTitle(), i.getType().name(), c.getClaimant().getFullName(),
                reporterView ? c.getClaimant().getEmail() : null, c.getMessage(), c.getAnswerGiven(),
                reporterView ? i.getVerificationAnswer() : null, i.getReporter().getFullName(),
                c.getStatus().name(), c.getCreatedAt());
    }

    // ---------------------------------------------------------- notifications

    @GetMapping("/notifications")
    public Map<String, Object> notificationList(HttpSession http) {
        User u = session.require(http);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("unread", notifications.unreadCount(u));
        out.put("items", notifications.forUser(u).stream()
                .map(n -> new NotificationDto(n.getId(), n.getMessage(), n.isSeen(), n.getCreatedAt())).toList());
        return out;
    }

    @PostMapping("/notifications/read")
    public Map<String, Boolean> markRead(HttpSession http) {
        notifications.markAllRead(session.require(http));
        return Map.of("ok", true);
    }
}
