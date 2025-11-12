package ru.skypro.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.skypro.homework.dto.user.PasswordChangeRequest;
import ru.skypro.homework.dto.user.UserProfileResponse;
import ru.skypro.homework.dto.user.UserProfileUpdateRequest;
import ru.skypro.homework.service.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Тестирование контроллера пользователей")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Nested
    @DisplayName("Тесты управления аватаром")
    class AvatarTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Успешная загрузка аватара пользователя")
        void uploadAvatar_Success() throws Exception {
            // Given
            MockMultipartFile image = new MockMultipartFile(
                    "image",
                    "avatar.jpg",
                    "image/jpeg",
                    "test image content".getBytes()
            );

            when(userService.uploadAvatar(eq("testuser"), any())).thenReturn(true);

            mockMvc.perform(multipart(HttpMethod.PATCH, "/users/me/image")
                            .file(image)
                            .with(csrf()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Тесты смены пароля")
    class PasswordChangeTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Успешная смена пароля")
        void changePassword_Success() throws Exception {
            // Given
            PasswordChangeRequest request = new PasswordChangeRequest();
            request.setCurrentPassword("oldPassword");
            request.setNewPassword("newPassword");

            when(userService.changePassword("testuser", "oldPassword", "newPassword"))
                    .thenReturn(true);

            // When & Then
            mockMvc.perform(post("/users/set_password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Ошибка смены пароля - неверный текущий пароль")
        void changePassword_Forbidden() throws Exception {
            // Given
            PasswordChangeRequest request = new PasswordChangeRequest();
            request.setCurrentPassword("wrongPassword");
            request.setNewPassword("newPassword");

            when(userService.changePassword("testuser", "wrongPassword", "newPassword"))
                    .thenReturn(false);

            // When & Then
            mockMvc.perform(post("/users/set_password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Тесты управления профилем")
    class ProfileTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Успешное получение профиля пользователя")
        void getProfile_Success() throws Exception {
            // Given
            UserProfileResponse profile = new UserProfileResponse();
            profile.setId(1L);
            profile.setEmail("testuser@example.com");
            profile.setFirstName("John");
            profile.setLastName("Doe");
            profile.setPhone("+79991234567");
            profile.setRole("USER");

            when(userService.getUserProfile("testuser")).thenReturn(profile);

            // When & Then
            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("testuser@example.com"))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Профиль пользователя не найден")
        void getProfile_NotFound() throws Exception {
            // Given
            when(userService.getUserProfile("testuser")).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Успешное обновление профиля пользователя")
        void updateProfile_Success() throws Exception {
            // Given
            UserProfileUpdateRequest updateRequest = new UserProfileUpdateRequest();
            updateRequest.setFirstName("Jane");
            updateRequest.setLastName("Smith");
            updateRequest.setPhone("+79991234567");

            UserProfileResponse updatedProfile = new UserProfileResponse();
            updatedProfile.setId(1L);
            updatedProfile.setEmail("testuser@example.com");
            updatedProfile.setFirstName("Jane");
            updatedProfile.setLastName("Smith");
            updatedProfile.setPhone("+79991234567");
            updatedProfile.setRole("USER");

            when(userService.updateUserProfile(eq("testuser"), any(UserProfileUpdateRequest.class)))
                    .thenReturn(updatedProfile);

            // When & Then
            mockMvc.perform(patch("/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.lastName").value("Smith"));
        }
    }
}