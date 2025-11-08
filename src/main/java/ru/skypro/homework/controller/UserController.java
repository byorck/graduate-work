package ru.skypro.homework.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.dto.user.PasswordChangeRequest;
import ru.skypro.homework.dto.user.UserProfileResponse;
import ru.skypro.homework.dto.user.UserProfileUpdateRequest;
import ru.skypro.homework.service.UserService;

import java.io.IOException;

@RestController
@RequestMapping("users")
@RequiredArgsConstructor
@Tag(name = "Пользователи", description = "API для работы с данными авторизированного пользователя")
public class UserController {
    private final UserService userService;

    /**
     * Загрузка или обновление аватара текущего пользователя
     */
    @PatchMapping(value = "/me/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Обновление аватара авторизованного пользователя")
    public ResponseEntity<?> uploadAvatar(
            Authentication authentication,
            @RequestParam("image") MultipartFile image) throws IOException { // Изменить на "image"
        String username = authentication.getName();
        boolean success = userService.uploadAvatar(username, image);
        return success ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /**
     * Смена пароля текущего пользователя
     */
    @PostMapping("/set_password")
    @Operation(summary = "Обновление пароля")
    public ResponseEntity<?> changePassword(Authentication auth, @RequestBody PasswordChangeRequest request) {
        String username = auth.getName();
        boolean success = userService.changePassword(username, request.getCurrentPassword(), request.getNewPassword());
        return success ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * Получение профиля текущего пользователя
     */
    @GetMapping("/me")
    @Operation(summary = "Получение информации об авторизованном пользователе")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        String username = authentication.getName();
        UserProfileResponse profile = userService.getUserProfile(username);
        return profile != null ? ResponseEntity.ok(profile) : ResponseEntity.notFound().build();
    }

    /**
     * Обновление информации профиля текущего пользователя
     */
    @PatchMapping("/me")
    @Operation(summary = "Обновление информации об авторизованном пользователе")
    public ResponseEntity<?> updateProfile(Authentication authentication,
                                           @RequestBody UserProfileUpdateRequest request) {
        UserProfileResponse updatedUser = userService.updateUserProfile(authentication.getName(), request);
        return updatedUser != null ? ResponseEntity.ok(updatedUser) : ResponseEntity.notFound().build();
    }
}