package com.lostfound.web;

import com.lostfound.model.User;
import com.lostfound.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Keeps the signed-in user's id in the HTTP session. */
@Component
public class SessionUser {

    private static final String KEY = "userId";
    private final AuthService auth;

    public SessionUser(AuthService auth) {
        this.auth = auth;
    }

    public void signIn(HttpSession session, User user) {
        session.setAttribute(KEY, user.getId());
    }

    /** The signed-in user, or null. */
    public User current(HttpSession session) {
        Object id = session.getAttribute(KEY);
        if (id == null) return null;
        try {
            return auth.byId((Long) id);
        } catch (java.util.NoSuchElementException e) {
            session.invalidate();
            return null;
        }
    }

    public User require(HttpSession session) {
        User u = current(session);
        if (u == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in first.");
        return u;
    }
}
