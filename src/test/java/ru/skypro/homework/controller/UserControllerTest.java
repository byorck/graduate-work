package ru.skypro.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@DisplayName("Тестирование UserController")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private final String USERNAME = "test@example.com";

    private UserProfileResponse testProfile;

    @BeforeEach
    void setUp() {
        testProfile = createUserProfile();
    }

    @Nested
    @DisplayName("Тесты загрузки аватара")
    class UploadAvatarTests {

        @Test
        @WithMockUser(username = USERNAME)
        @DisplayName("Успешная загрузка аватара")
        void uploadAvatar_WhenSuccess_ShouldReturnOk() throws Exception {
            // Given
            MockMultipartFile image = new MockMultipartFile(
                    "image",
                    "avatar.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "avatar content".getBytes()
            );

            when(userService.uploadAvatar(eq(USERNAME), any()))
                    .thenReturn(true);

            // When & Then - Используем multipart для PATCH запроса
            mockMvc.perform(multipart("/users/me/image")
                            .file(image)
                            .with(csrf())
                            .with(request -> {
                                request.setMethod("PATCH");
                                return request;
                            })
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = USERNAME)
        @DisplayName("Загрузка аватара для несуществующего пользователя")
        void uploadAvatar_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
            // Given
            MockMultipartFile image = new MockMultipartFile(
                    "image",
                    "avatar.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "avatar content".getBytes()
            );

            when(userService.uploadAvatar(eq(USERNAME), any()))
                    .thenReturn(false);

            // When & Then - Используем multipart для PATCH запроса
            mockMvc.perform(multipart("/users/me/image")
                            .file(image)
                            .with(csrf())
                            .with(request -> {
                                request.setMethod("PATCH");
                                return request;
                            })
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Тесты смены пароля")
    class ChangePasswordTests {

        @Test
        @WithMockUser(username = USERNAME)
        @DisplayName("Успешная смена пароля")
        void changePassword_WhenSuccess_ShouldReturnOk() throws Exception {
            // Given
            PasswordChangeRequest request = new PasswordChangeRequest();
            request.setCurrentPassword("oldPassword");
            request.setNewPassword("newPassword");

            when(userService.changePassword(USERNAME, "oldPassword", "newPassword"))
                    .thenReturn(true);

            // When & Then
            mockMvc.perform(post("/users/set_password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = USERNAME)
        @DisplayName("Смена пароля с неправильным текущим паролем")
        void changePassword_WhenWrongCurrentPassword_ShouldReturnForbidden() throws Exception {
            // Given
            PasswordChangeRequest request = new PasswordChangeRequest();
            request.setCurrentPassword("wrongPassword");
            request.setNewPassword("newPassword");

            when(userService.changePassword(USERNAME, "wrongPassword", "newPassword"))
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
    @DisplayName("Тесты работы с профилем пользователя")
    class ProfileTests {

        @Test
        @WithMockUser(username = USERNAME)
        @DisplayName("Получение профиля существующего пользователя")
        void getProfile_WhenUserExists_ShouldReturnOk() throws Exception {
            // Given
            when(userService.getUserProfile(USERNAME)).thenReturn(testProfile);

            // When & Then
            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value(USERNAME))
                    .andExpect(jsonPath("$.firstName").value("John"));
        }

        @Test
        @WithMockUser(username = USERNAME)
        @DisplayName("Получение профиля несуществующего пользователя")
        void getProfile_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
            // Given
            when(userService.getUserProfile(USERNAME)).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = USERNAME)
        @DisplayName("Успешное обновление профиля")
        void updateProfile_WhenSuccess_ShouldReturnOk() throws Exception {
            // Given
            UserProfileUpdateRequest updateRequest = new UserProfileUpdateRequest();
            updateRequest.setFirstName("UpdatedJohn");
            updateRequest.setLastName("UpdatedDoe");
            updateRequest.setPhone("+79998887766");

            UserProfileResponse updatedProfile = createUserProfile();
            updatedProfile.setFirstName("UpdatedJohn");

            when(userService.updateUserProfile(eq(USERNAME), any(UserProfileUpdateRequest.class)))
                    .thenReturn(updatedProfile);

            // When & Then
            mockMvc.perform(patch("/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("UpdatedJohn"));
        }

        @Test
        @WithMockUser(username = USERNAME)
        @DisplayName("Обновление профиля несуществующего пользователя")
        void updateProfile_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
            // Given
            UserProfileUpdateRequest updateRequest = new UserProfileUpdateRequest();
            updateRequest.setFirstName("UpdatedJohn");

            when(userService.updateUserProfile(eq(USERNAME), any(UserProfileUpdateRequest.class)))
                    .thenReturn(null);

            // When & Then
            mockMvc.perform(patch("/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNotFound());
        }
    }

    private UserProfileResponse createUserProfile() {
        UserProfileResponse profile = new UserProfileResponse();
        profile.setId(1L);
        profile.setEmail(USERNAME);
        profile.setFirstName("John");
        profile.setLastName("Doe");
        profile.setPhone("+79998887766");
        profile.setRole("USER");
        profile.setImage("/users/1/avatar");
        return profile;
    }
}