package ru.skypro.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.skypro.homework.dto.user.PasswordChangeRequest;
import ru.skypro.homework.dto.user.UserProfileResponse;
import ru.skypro.homework.dto.user.UserProfileUpdateRequest;
import ru.skypro.homework.service.UserService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тестовый класс для UserController.
 * Проверяет endpoints для работы с пользователями
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Тесты профиля пользователя")
    class ProfileTests {

        @Test
        @WithMockUser
        @DisplayName("Получение профиля - когда пользователь существует")
        void getProfile_WhenUserExists_ShouldReturnProfile() throws Exception {
            // Given
            UserProfileResponse profile = new UserProfileResponse();
            profile.setId(1L);
            profile.setEmail("user@example.com");
            profile.setFirstName("John");
            profile.setLastName("Doe");
            profile.setPhone("+123456789");
            profile.setRole("USER");

            when(userService.getUserProfile(anyString())).thenReturn(profile);

            // When & Then
            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("user@example.com"))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"));
        }

        @Test
        @WithMockUser
        @DisplayName("Получение профиля - когда пользователь не существует")
        void getProfile_WhenUserNotExists_ShouldReturnNotFound() throws Exception {
            // Given
            when(userService.getUserProfile(anyString())).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("Обновление профиля - когда пользователь существует")
        void updateProfile_WhenUserExists_ShouldReturnOk() throws Exception {
            // Given
            UserProfileUpdateRequest request = new UserProfileUpdateRequest();
            request.setFirstName("Jane");
            request.setLastName("Smith");
            request.setPhone("+987654321");

            when(userService.updateUserProfile(anyString(), any(UserProfileUpdateRequest.class))).thenReturn(true);

            // When & Then
            mockMvc.perform(patch("/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Обновление профиля - когда пользователь не существует")
        void updateProfile_WhenUserNotExists_ShouldReturnNotFound() throws Exception {
            // Given
            UserProfileUpdateRequest request = new UserProfileUpdateRequest();
            request.setFirstName("Jane");
            request.setLastName("Smith");
            request.setPhone("+987654321");

            when(userService.updateUserProfile(anyString(), any(UserProfileUpdateRequest.class))).thenReturn(false);

            // When & Then
            mockMvc.perform(patch("/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Тесты смены пароля")
    class PasswordTests {

        @Test
        @WithMockUser
        @DisplayName("Смена пароля - когда текущий пароль корректен")
        void changePassword_WhenCurrentPasswordCorrect_ShouldReturnOk() throws Exception {
            // Given
            PasswordChangeRequest request = new PasswordChangeRequest();
            request.setCurrentPassword("oldPassword");
            request.setNewPassword("newPassword");

            when(userService.changePassword(anyString(), eq("oldPassword"), eq("newPassword"))).thenReturn(true);

            // When & Then
            mockMvc.perform(post("/users/set_password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Смена пароля - когда текущий пароль неверен")
        void changePassword_WhenCurrentPasswordIncorrect_ShouldReturnForbidden() throws Exception {
            // Given
            PasswordChangeRequest request = new PasswordChangeRequest();
            request.setCurrentPassword("wrongPassword");
            request.setNewPassword("newPassword");

            when(userService.changePassword(anyString(), eq("wrongPassword"), eq("newPassword"))).thenReturn(false);

            // When & Then
            mockMvc.perform(post("/users/set_password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Тесты загрузки аватара")
    class AvatarTests {

        @Test
        @WithMockUser
        @DisplayName("Загрузка аватара - когда пользователь существует")
        void uploadAvatar_WhenUserExists_ShouldReturnOk() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "avatar",
                    "avatar.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "test image content".getBytes()
            );

            when(userService.uploadAvatar(anyString(), any())).thenReturn(true);

            // When & Then
            mockMvc.perform(multipart("/users/me/image")
                            .file(file)
                            .with(csrf())
                            .with(request -> {
                                request.setMethod("PATCH");
                                return request;
                            })
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Загрузка аватара - когда пользователь не существует")
        void uploadAvatar_WhenUserNotExists_ShouldReturnNotFound() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "avatar",
                    "avatar.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "test image content".getBytes()
            );

            when(userService.uploadAvatar(anyString(), any())).thenReturn(false);

            // When & Then
            mockMvc.perform(multipart("/users/me/image")
                            .file(file)
                            .with(csrf())
                            .with(request -> {
                                request.setMethod("PATCH");
                                return request;
                            })
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isNotFound());
        }
    }
}