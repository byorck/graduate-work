package ru.skypro.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.skypro.homework.dto.ad.AdCreateRequestDTO;
import ru.skypro.homework.dto.ad.AdFullResponseDTO;
import ru.skypro.homework.dto.ad.AdUpdateRequestDTO;
import ru.skypro.homework.entity.Ad;
import ru.skypro.homework.entity.User;
import ru.skypro.homework.service.AdService;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Тестирование контроллера объявлений")
class AdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdService adService;

    @Nested
    @DisplayName("Тесты создания объявлений")
    class CreateAdTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Успешное создание объявления")
        void createAd_Success() throws Exception {
            // Given
            AdCreateRequestDTO requestDTO = new AdCreateRequestDTO();
            requestDTO.setTitle("Test Ad");
            requestDTO.setPrice(1000);
            requestDTO.setDescription("Test Description");

            User user = new User();
            user.setId(1L);
            user.setUsername("testuser");

            Ad ad = new Ad();
            ad.setId(1L);
            ad.setTitle("Test Ad");
            ad.setPrice(1000);
            ad.setDescription("Test Description");
            ad.setUser(user);

            AdFullResponseDTO responseDTO = new AdFullResponseDTO(ad);

            MockMultipartFile properties = new MockMultipartFile(
                    "properties",
                    "",
                    "application/json",
                    objectMapper.writeValueAsBytes(requestDTO)
            );
            MockMultipartFile image = new MockMultipartFile(
                    "image",
                    "test.jpg",
                    "image/jpeg",
                    "test image content".getBytes()
            );

            when(adService.createAd(anyString(), anyString(), anyInt(), anyString(), any()))
                    .thenReturn(responseDTO);

            // When & Then
            mockMvc.perform(multipart("/ads")
                            .file(properties)
                            .file(image)
                            .with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.pk").value(1))
                    .andExpect(jsonPath("$.title").value("Test Ad"));
        }
    }

    @Nested
    @DisplayName("Тесты получения объявлений")
    class GetAdTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Успешное получение всех объявлений")
        void getAllAds_Success() throws Exception {
            // Given
            Map<String, Object> response = new HashMap<>();
            response.put("count", 1);
            response.put("results", new Object[]{});

            when(adService.getAllAds()).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/ads"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(1));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Успешное получение объявления по ID")
        void getAdById_Success() throws Exception {
            // Given
            User user = new User();
            user.setId(1L);

            Ad ad = new Ad();
            ad.setId(1L);
            ad.setTitle("Test Ad");
            ad.setUser(user);

            AdFullResponseDTO responseDTO = new AdFullResponseDTO(ad);

            when(adService.getAdById(1L)).thenReturn(responseDTO);

            // When & Then
            mockMvc.perform(get("/ads/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pk").value(1))
                    .andExpect(jsonPath("$.title").value("Test Ad"));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Объявление по ID не найдено")
        void getAdById_NotFound() throws Exception {
            // Given
            when(adService.getAdById(1L)).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/ads/1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Успешное получение собственных объявлений пользователя")
        void getMyAds_Success() throws Exception {
            // Given
            Map<String, Object> response = new HashMap<>();
            response.put("count", 2);
            response.put("results", new Object[]{});

            when(adService.getUserAds("testuser")).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/ads/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(2));
        }
    }

    @Nested
    @DisplayName("Тесты управления объявлениями")
    class ManageAdTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Успешное удаление объявления")
        void deleteAd_Success() throws Exception {
            // Given
            when(adService.deleteAd(1L, "testuser")).thenReturn(true);

            // When & Then
            mockMvc.perform(delete("/ads/1").with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Успешное обновление объявления")
        void updateAd_Success() throws Exception {
            // Given
            AdUpdateRequestDTO updateRequest = new AdUpdateRequestDTO();
            updateRequest.setTitle("Updated Title");
            updateRequest.setPrice(1500);
            updateRequest.setDescription("Updated Description");

            User user = new User();
            user.setId(1L);

            Ad ad = new Ad();
            ad.setId(1L);
            ad.setTitle("Updated Title");
            ad.setUser(user);

            AdFullResponseDTO updatedAd = new AdFullResponseDTO(ad);

            when(adService.updateAd(eq(1L), any(AdUpdateRequestDTO.class), eq("testuser")))
                    .thenReturn(updatedAd);

            // When & Then
            mockMvc.perform(patch("/ads/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pk").value(1))
                    .andExpect(jsonPath("$.title").value("Updated Title"));
        }
    }
}