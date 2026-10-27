package com.nptelmanager.service;

import com.nptelmanager.entity.User;
import com.nptelmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(User.Role.STUDENT);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    public User updateProfile(Long userId, User updatedUser) {
        User existing = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        existing.setFullName(updatedUser.getFullName());
        existing.setPhone(updatedUser.getPhone());
        existing.setCollege(updatedUser.getCollege());
        existing.setDepartment(updatedUser.getDepartment());
        existing.setRollNumber(updatedUser.getRollNumber());
        existing.setEmailNotificationsEnabled(updatedUser.isEmailNotificationsEnabled());
        return userRepository.save(existing);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("No account registered with email: " + email));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
