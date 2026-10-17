package com.lostfound.service;

import com.lostfound.model.Role;
import com.lostfound.model.User;
import com.lostfound.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

/** Registration and login. Passwords are never stored: only a salted PBKDF2 hash. */
@Service
@Transactional
public class AuthService {

    private final UserRepository users;

    public AuthService(UserRepository users) {
        this.users = users;
    }

    public User register(String username, String fullName, String email, String password, Role role) {
        username = username == null ? "" : username.trim();
        if (username.length() < 3) throw new IllegalArgumentException("Username must be at least 3 characters.");
        if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("Full name is required.");
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("Please enter a valid email address.");
        if (password == null || password.length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters.");
        if (users.findByUsername(username).isPresent()) throw new IllegalArgumentException("That username is already taken.");

        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        User u = new User();
        u.setUsername(username);
        u.setFullName(fullName.trim());
        u.setEmail(email.trim());
        u.setSalt(Base64.getEncoder().encodeToString(salt));
        u.setPasswordHash(hash(password, salt));
        u.setRole(role == null ? Role.USER : role);
        return users.save(u);
    }

    public User register(String username, String fullName, String email, String password) {
        return register(username, fullName, email, password, Role.USER);
    }

    @Transactional(readOnly = true)
    public User login(String username, String password) {
        User u = users.findByUsername(username == null ? "" : username.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));
        byte[] salt = Base64.getDecoder().decode(u.getSalt());
        String attempt = hash(password == null ? "" : password, salt);
        if (!MessageDigest.isEqual(attempt.getBytes(), u.getPasswordHash().getBytes()))
            throw new IllegalArgumentException("Invalid username or password.");
        return u;
    }

    @Transactional(readOnly = true)
    public User byId(Long id) {
        return users.findById(id).orElseThrow(() -> new java.util.NoSuchElementException("User not found"));
    }

    @Transactional(readOnly = true)
    public List<User> allUsers() {
        return users.findAll();
    }

    private static String hash(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65_536, 256);
            byte[] bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing unavailable", e);
        }
    }
}
