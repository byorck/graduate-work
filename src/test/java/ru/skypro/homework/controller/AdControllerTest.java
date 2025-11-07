package ru.skypro.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.skypro.homework.dto.ad.AdFullResponseDTO;
import ru.skypro.homework.dto.ad.AdUpdateRequestDTO;
import ru.skypro.homework.service.AdService;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тестовый класс для AdController.
 * Проверяет endpoints для работы с объявлениями
 */
@WebMvcTest(AdController.class)
@DisplayName("Тестирование контроллера объявлений")
class AdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdService adService;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Тесты получения объявлений")
    class GetAdsTests {

        @Test
        @WithMockUser
        @DisplayName("Получение всех объявлений - должен вернуть список объявлений")
        void getAllAds_ShouldReturnAds() throws Exception {
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
        @WithMockUser
        @DisplayName("Получение объявления по ID - когда объявление существует")
        void getAdById_WhenAdExists_ShouldReturnAd() throws Exception {
            // Given
            AdFullResponseDTO ad = new AdFullResponseDTO();
            ad.setPk(1L);
            ad.setTitle("Test Ad");
            when(adService.getAdById(1L)).thenReturn(ad);

            // When & Then
            mockMvc.perform(get("/ads/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pk").value(1))
                    .andExpect(jsonPath("$.title").value("Test Ad"));
        }

        @Test
        @WithMockUser
        @DisplayName("Получение объявления по ID - когда объявление не существует")
        void getAdById_WhenAdNotExists_ShouldReturnNotFound() throws Exception {
            // Given
            when(adService.getAdById(1L)).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/ads/1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("Получение объявлений текущего пользователя - должен вернуть список")
        void getMyAds_ShouldReturnUserAds() throws Exception {
            // Given
            Map<String, Object> response = new HashMap<>();
            response.put("count", 1);
            response.put("results", new Object[]{});
            when(adService.getUserAds(anyString())).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/ads/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(1));
        }
    }

    @Nested
    @DisplayName("Тесты управления объявлениями")
    class ManageAdsTests {

        @Test
        @WithMockUser
        @DisplayName("Создание объявления - с валидными данными")
        void createAd_WithValidData_ShouldReturnCreated() throws Exception {
            // Given
            AdFullResponseDTO responseDTO = new AdFullResponseDTO();
            responseDTO.setPk(1L);
            responseDTO.setTitle("New Ad");

            MockMultipartFile imageFile = new MockMultipartFile(
                    "image",
                    "test.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "test image content".getBytes()
            );

            when(adService.createAd(anyString(), anyString(), anyInt(), anyString(), any()))
                    .thenReturn(responseDTO);

            // When & Then
            mockMvc.perform(multipart("/ads")
                            .file(imageFile)
                            .param("title", "New Ad")
                            .param("price", "100")
                            .param("description", "Test Description")
                            .with(csrf())
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.pk").value(1))
                    .andExpect(jsonPath("$.title").value("New Ad"));
        }

        @Test
        @WithMockUser
        @DisplayName("Удаление объявления - когда пользователь имеет права")
        void deleteAd_WhenUserHasPermission_ShouldReturnNoContent() throws Exception {
            // Given
            when(adService.deleteAd(eq(1L), anyString())).thenReturn(true);

            // When & Then
            mockMvc.perform(delete("/ads/1").with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser
        @DisplayName("Удаление объявления - когда пользователь не имеет прав")
        void deleteAd_WhenUserNoPermission_ShouldReturnNotFound() throws Exception {
            // Given
            when(adService.deleteAd(eq(1L), anyString())).thenReturn(false);

            // When & Then
            mockMvc.perform(delete("/ads/1").with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("Обновление объявления - когда пользователь имеет права")
        void updateAd_WhenUserHasPermission_ShouldReturnNoContent() throws Exception {
            // Given
            AdUpdateRequestDTO updateRequest = new AdUpdateRequestDTO();
            updateRequest.setTitle("Updated Title");
            updateRequest.setPrice(200);
            updateRequest.setDescription("Updated Description");

            when(adService.updateAd(eq(1L), any(AdUpdateRequestDTO.class), anyString())).thenReturn(true);

            // When & Then
            mockMvc.perform(patch("/ads/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNoContent());
        }
    }
}