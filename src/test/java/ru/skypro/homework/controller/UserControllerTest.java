package ru.skypro.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.dto.user.PasswordChangeRequest;
import ru.skypro.homework.dto.user.UserProfileResponse;
import ru.skypro.homework.dto.user.UserProfileUpdateRequest;
import ru.skypro.homework.entity.User;
import ru.skypro.homework.service.AvatarService;
import ru.skypro.homework.service.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AvatarService avatarService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "test@mail.com")
    void uploadAvatar_WhenValidFile_ShouldReturnOk() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        User user = new User();
        user.setUsername("test@mail.com");

        when(userService.findUser("test@mail.com")).thenReturn(user);
        doNothing().when(avatarService).uploadAvatar(any(User.class), any(MultipartFile.class));

        // Используем multipartPatch вместо multipart
        mockMvc.perform(multipartPatch("/users/me/image")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test@mail.com")
    void uploadAvatar_WhenFileTooBig_ShouldReturnBadRequest() throws Exception {
        byte[] largeFile = new byte[5 * 1024 * 1024 + 1]; // 5MB + 1 byte
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "large.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                largeFile
        );

        mockMvc.perform(multipartPatch("/users/me/image")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("File is too big"));
    }

    @Test
    @WithMockUser(username = "test@mail.com")
    void uploadAvatar_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        when(userService.findUser("test@mail.com")).thenReturn(null);

        mockMvc.perform(multipartPatch("/users/me/image")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"));
    }

    @Test
    @WithMockUser(username = "test@mail.com")
    void changePassword_WhenValidRequest_ShouldReturnOk() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword");

        when(userService.checkOldPassword("test@mail.com", "oldPassword")).thenReturn(true);
        doNothing().when(userService).updatePassword("test@mail.com", "newPassword");

        mockMvc.perform(post("/users/set_password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password changed successfully"));
    }

    @Test
    @WithMockUser(username = "test@mail.com")
    void changePassword_WhenInvalidOldPassword_ShouldReturnForbidden() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPassword");

        when(userService.checkOldPassword("test@mail.com", "wrongPassword")).thenReturn(false);

        mockMvc.perform(post("/users/set_password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Old password is incorrect"));
    }

    @Test
    @WithMockUser(username = "test@mail.com")
    void getProfile_WhenUserExists_ShouldReturnProfile() throws Exception {
        UserProfileResponse profile = new UserProfileResponse();
        profile.setId(1L);
        profile.setEmail("test@mail.com");
        profile.setFirstName("John");
        profile.setLastName("Doe");
        profile.setPhone("+1234567890");

        when(userService.getUserProfile("test@mail.com")).thenReturn(profile);

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("test@mail.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.phone").value("+1234567890"));
    }

    @Test
    @WithMockUser(username = "test@mail.com")
    void getProfile_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
        when(userService.getUserProfile("test@mail.com")).thenReturn(null);

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "test@mail.com")
    void updateProfile_WhenValidRequest_ShouldReturnOk() throws Exception {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setPhone("+0987654321");

        User updatedUser = new User();
        updatedUser.setUsername("test@mail.com");

        when(userService.updateUserProfile(eq("test@mail.com"), any(UserProfileUpdateRequest.class)))
                .thenReturn(updatedUser);

        mockMvc.perform(patch("/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test@mail.com")
    void updateProfile_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setPhone("+0987654321");

        when(userService.updateUserProfile(eq("test@mail.com"), any(UserProfileUpdateRequest.class)))
                .thenReturn(null);

        mockMvc.perform(patch("/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    private MockMultipartHttpServletRequestBuilder multipartPatch(String url) {
        MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(url);
        builder.with(request -> {
            request.setMethod("PATCH");
            return request;
        });
        return builder;
    }
}