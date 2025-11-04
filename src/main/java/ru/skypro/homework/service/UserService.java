package ru.skypro.homework.service;

import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.dto.Role;
import ru.skypro.homework.dto.user.UserProfileResponse;
import ru.skypro.homework.dto.user.UserProfileUpdateRequest;
import ru.skypro.homework.entity.Ad;
import ru.skypro.homework.entity.Avatar;
import ru.skypro.homework.entity.User;
import ru.skypro.homework.repository.UserRepository;

import java.io.IOException;

@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarService avatarService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AvatarService avatarService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.avatarService = avatarService;
    }

    public User findUser(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public boolean checkOldPassword(String username, String oldPassword) {
        User user = findUser(username);
        return user != null && passwordEncoder.matches(oldPassword, user.getPassword());
    }

    @Transactional
    public void updatePassword(String username, String newPassword) {
        User user = findUser(username);
        if (user != null) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        }
    }

    public UserProfileResponse getUserProfile(String username) {
        User user = findUser(username);
        if (user == null) {
            return null;
        }
        UserProfileResponse profile = new UserProfileResponse();
        profile.setId(user.getId());
        profile.setEmail(user.getUsername());
        profile.setFirstName(user.getFirstName());
        profile.setLastName(user.getLastName());
        profile.setPhone(user.getPhone());
        profile.setRole(user.getRole().name());
        Avatar avatar = avatarService.findAvatar(username);
        if (avatar != null) {
            profile.setImage("/users/" + username + "/avatar");
        }
        return profile;
    }

    public User updateUserProfile(String username, UserProfileUpdateRequest request) {
        User user = findUser(username);
        if (user == null) {
            return null;
        }
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        return userRepository.save(user);
    }

    public void uploadAvatarForUser(String username, MultipartFile file) throws IOException {
        User user = findUser(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        avatarService.uploadAvatar(user, file);
    }

    public boolean hasPermission(Ad ad, Authentication authentication) {
        String username = authentication.getName();
        User user = findUser(username);

        if (user.getRole() == Role.ADMIN) {
            return true;
        }

        return ad.getUser().getUsername().equals(username);
    }
}
