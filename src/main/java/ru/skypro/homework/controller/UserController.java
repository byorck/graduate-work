package ru.skypro.homework.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.dto.user.PasswordChangeRequest;
import ru.skypro.homework.dto.user.UserProfileResponse;
import ru.skypro.homework.dto.user.UserProfileUpdateRequest;
import ru.skypro.homework.entity.User;
import ru.skypro.homework.service.AvatarService;
import ru.skypro.homework.service.UserService;

import java.io.IOException;

@RestController
@RequestMapping("users")
@Tag(name = "Пользователи")
public class UserController {
    private final UserService userService;
    private final AvatarService avatarService;

    public UserController(UserService userService, AvatarService avatarService) {
        this.userService = userService;
        this.avatarService = avatarService;
    }

    /**
     * Загрузка или обновление аватара пользователя
     * @param authentication данные аутентификации текущего пользователя
     * @param avatar файл изображения для аватара
     * @return 200 OK при успешной загрузке, 400 если файл слишком большой, 404 если пользователь не найден
     * @throws IOException при ошибках загрузки файла
     */
    @PatchMapping(value = "/me/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Обновление аватара авторизованного пользователя")
    public ResponseEntity<?> uploadAvatar(Authentication authentication, @RequestParam MultipartFile avatar) throws IOException {
        String username = authentication.getName();
        if (avatar.getSize() >= 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body("File is too big");
        }
        User user = userService.findUser(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        avatarService.uploadAvatar(user, avatar);
        return ResponseEntity.ok().build();
    }

    /**
     * Смена пароля пользователя
     * @param auth данные аутентификации текущего пользователя
     * @param request DTO с текущим и новым паролем
     * @return 200 OK при успешной смене пароля, 403 если текущий пароль неверен
     */
    @PostMapping("/set_password")
    @Operation(summary = "Обновление пароля")
    public ResponseEntity<?> changePassword(Authentication auth, @RequestBody PasswordChangeRequest request) {
        String username = auth.getName();
        if (!userService.checkOldPassword(username, request.getCurrentPassword())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Old password is incorrect");
        }
        userService.updatePassword(username, request.getNewPassword());
        return ResponseEntity.ok("Password changed successfully");
    }

    /**
     * Получение профиля текущего пользователя
     * @param authentication данные аутентификации текущего пользователя
     * @return профиль пользователя или 404 если не найден
     */
    @GetMapping("/me")
    @Operation(summary = "Получение информации об авторизованном пользователе")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        String username = authentication.getName();
        UserProfileResponse profile = userService.getUserProfile(username);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(profile);
    }

    /**
     * Обновление информации профиля пользователя
     * @param authentication данные аутентификации текущего пользователя
     * @param request DTO с обновляемыми данными профиля
     * @return 200 OK при успешном обновлении, 404 если пользователь не найден
     */
    @PatchMapping("/me")
    @Operation(summary = "Обновление информации об авторизованном пользователе")
    public ResponseEntity<?> updateProfile(Authentication authentication, @RequestBody UserProfileUpdateRequest request) {
        String username = authentication.getName();
        User updatedUser = userService.updateUserProfile(username, request);
        if (updatedUser == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }
}
