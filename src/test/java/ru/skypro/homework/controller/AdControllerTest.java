package ru.skypro.homework.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.config.TestConfig;
import ru.skypro.homework.config.WebSecurityConfig;
import ru.skypro.homework.dto.Role;
import ru.skypro.homework.dto.ad.AdCreateRequestDTO;
import ru.skypro.homework.dto.ad.AdFullResponseDTO;
import ru.skypro.homework.dto.ad.AdShortResponseDTO;
import ru.skypro.homework.dto.ad.AdUpdateRequestDTO;
import ru.skypro.homework.entity.Ad;
import ru.skypro.homework.entity.User;
import ru.skypro.homework.service.AdService;
import ru.skypro.homework.service.CustomUserDetailsService;
import ru.skypro.homework.service.UserService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdController.class)
@Import({WebSecurityConfig.class, TestConfig.class})
@WithMockUser
class AdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdService adService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;
    private User adminUser;
    private Ad testAd;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("test@mail.com");
        testUser.setPassword("password");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPhone("+123456789");
        testUser.setRole(Role.USER);

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin@mail.com");
        adminUser.setRole(Role.ADMIN);

        testAd = new Ad();
        testAd.setId(1L);
        testAd.setTitle("Test Ad");
        testAd.setPrice(1000);
        testAd.setDescription("Test Description");
        testAd.setUser(testUser);
    }

    @Test
    @WithMockUser(username = "test@mail.com")
    void createAd_ShouldReturnCreated() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "test image content".getBytes()
        );

        when(userService.findUser("test@mail.com")).thenReturn(testUser);
        when(adService.createAd(any(User.class), any(AdCreateRequestDTO.class), any(MultipartFile.class)))
                .thenReturn(testAd);

        mockMvc.perform(multipart("/ads")
                        .file(imageFile)
                        .param("title", "Test Ad")
                        .param("price", "1000")
                        .param("description", "Test Description")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pk").value(1L))
                .andExpect(jsonPath("$.title").value("Test Ad"));
    }

    @Test
    void getAllAds_ShouldReturnAdsList() throws Exception {
        AdShortResponseDTO shortResponse = new AdShortResponseDTO();
        shortResponse.setPk(1L);
        shortResponse.setTitle("Test Ad");
        shortResponse.setPrice(1000);

        when(adService.findAll()).thenReturn(List.of(shortResponse));

        mockMvc.perform(get("/ads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.results[0].pk").value(1L))
                .andExpect(jsonPath("$.results[0].title").value("Test Ad"));
    }

    @Test
    void getAdById_WhenAdExists_ShouldReturnAd() throws Exception {
        AdFullResponseDTO fullResponse = new AdFullResponseDTO(testAd);

        when(adService.findById(1L)).thenReturn(fullResponse);

        mockMvc.perform(get("/ads/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pk").value(1L))
                .andExpect(jsonPath("$.title").value("Test Ad"));
    }

    @Test
    void getAdById_WhenAdNotExists_ShouldReturnNotFound() throws Exception {
        when(adService.findById(1L)).thenReturn(null);

        mockMvc.perform(get("/ads/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "test@mail.com")
    void deleteAd_WhenUserIsOwner_ShouldReturnNoContent() throws Exception {
        when(adService.getAdEntityById(1L)).thenReturn(testAd);
        when(userService.findUser("test@mail.com")).thenReturn(testUser);
        when(adService.deleteById(1L)).thenReturn(true);

        doReturn(true).when(userService).hasPermission(any(Ad.class), any(Authentication.class));

        mockMvc.perform(delete("/ads/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "other@mail.com")
    void deleteAd_WhenUserIsNotOwner_ShouldReturnForbidden() throws Exception {
        User otherUser = new User();
        otherUser.setUsername("other@mail.com");
        otherUser.setRole(Role.USER);

        when(adService.getAdEntityById(1L)).thenReturn(testAd);
        when(userService.findUser("other@mail.com")).thenReturn(otherUser);

        mockMvc.perform(delete("/ads/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@mail.com", roles = "ADMIN")
    void deleteAd_WhenUserIsAdmin_ShouldReturnNoContent() throws Exception {
        when(adService.getAdEntityById(1L)).thenReturn(testAd);
        when(userService.findUser("admin@mail.com")).thenReturn(adminUser);
        when(adService.deleteById(1L)).thenReturn(true);

        doReturn(true).when(userService).hasPermission(any(Ad.class), any(Authentication.class));

        mockMvc.perform(delete("/ads/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test@mail.com")
    void updateAd_WhenUserIsOwner_ShouldReturnNoContent() throws Exception {
        when(adService.getAdEntityById(1L)).thenReturn(testAd);
        when(userService.findUser("test@mail.com")).thenReturn(testUser);
        when(adService.updateAd(eq(1L), any(AdUpdateRequestDTO.class))).thenReturn(true);

        String jsonContent = "{\"title\": \"Updated Ad\", \"price\": 1500, \"description\": \"Updated Description\"}";

        doReturn(true).when(userService).hasPermission(any(Ad.class), any(Authentication.class));

        mockMvc.perform(patch("/ads/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test@mail.com")
    void getMyAds_ShouldReturnUserAds() throws Exception {
        AdShortResponseDTO shortResponse = new AdShortResponseDTO();
        shortResponse.setPk(1L);
        shortResponse.setTitle("My Ad");

        when(adService.findByUsername("test@mail.com")).thenReturn(List.of(shortResponse));

        mockMvc.perform(get("/ads/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.results[0].pk").value(1L));
    }

    @Test
    @WithMockUser(username = "test@mail.com")
    void updateAdImage_WhenUserIsOwner_ShouldReturnNoContent() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "test.jpg", "image/jpeg", "test image content".getBytes()
        );

        when(adService.getAdEntityById(1L)).thenReturn(testAd);
        when(userService.findUser("test@mail.com")).thenReturn(testUser);

        doReturn(true).when(userService).hasPermission(any(Ad.class), any(Authentication.class));

        mockMvc.perform(multipart("/ads/1/image")
                        .file(imageFile)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}