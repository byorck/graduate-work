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
import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.dto.ad.AdCreateRequestDTO;
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

@WebMvcTest(AdController.class)
@DisplayName("Тестирование AdController")
class AdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdService adService;

    private final String USERNAME = "test@example.com";

    private AdFullResponseDTO testAdResponse;
    private Map<String, Object> testAdsResponse;

    @BeforeEach
    void setUp() {
        testAdResponse = new AdFullResponseDTO();
        testAdResponse.setPk(1L);
        testAdResponse.setTitle("Test Ad");
        testAdResponse.setPrice(1000);
        testAdResponse.setDescription("Test Description");

        testAdsResponse = new HashMap<>();
        testAdsResponse.put("count", 5);
        testAdsResponse.put("results", new Object[]{});
    }

    @Nested
    @DisplayName("Тесты создания объявлений")
    class CreateAdTests {

        @Test
        @WithMockUser(username = USERNAME)
        @DisplayName("Успешное создание объявления")
        void createAd_ShouldReturnCreated() throws Exception {
            // Given
            AdCreateRequestDTO requestDTO = new AdCreateRequestDTO();
            requestDTO.setTitle("Test Ad");
            requestDTO.setPrice(1000);
            requestDTO.setDescription("Test Description");

            MockMultipartFile properties = new MockMultipartFile(
                    "properties",
                    "",
                    MediaType.APPLICATION_JSON_VALUE,
                    objectMapper.writeValueAsBytes(requestDTO)
            );
            MockMultipartFile image = new MockMultipartFile(
                    "image",
                    "test.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "test image content".getBytes()
            );

            when(adService.createAd(eq(USERNAME), anyString(), anyInt(), anyString(), any(MultipartFile.class)))
                    .thenReturn(testAdResponse);

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
    class GetAdsTests {

        @Test
        @WithMockUser
        @DisplayName("Получение всех объявлений")
        void getAllAds_ShouldReturnOk() throws Exception {
            // Given
            when(adService.getAllAds()).thenReturn(testAdsResponse);

            // When & Then
            mockMvc.perform(get("/ads"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(5));
        }

        @Test
        @WithMockUser
        @DisplayName("Получение существующего объявления по ID")
        void getAdById_WhenAdExists_ShouldReturnOk() throws Exception {
            // Given
            when(adService.getAdById(1L)).thenReturn(testAdResponse);

            // When & Then
            mockMvc.perform(get("/ads/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pk").value(1));
        }

        @Test
        @WithMockUser
        @DisplayName("Получение несуществующего объявления по ID")
        void getAdById_WhenAdNotExists_ShouldReturnNotFound() throws Exception {
            // Given
            when(adService.getAdById(1L)).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/ads/1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = USERNAME)
        @DisplayName("Получение объявлений текущего пользователя")
        void getMyAds_ShouldReturnOk() throws Exception {
            // Given
            Map<String, Object> userAdsResponse = new HashMap<>();
            userAdsResponse.put("count", 3);
            userAdsResponse.put("results", new Object[]{});

            when(adService.getUserAds(USERNAME)).thenReturn(userAdsResponse);

            // When & Then
            mockMvc.perform(get("/ads/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(3));
        }
    }

    @Nested
    @DisplayName("Тесты удаления объявлений")
    class DeleteAdTests {

        @Test
        @WithMockUser(username = USERNAME)
        @DisplayName("Удаление объявления авторизованным пользователем")
        void deleteAd_WhenAuthorized_ShouldReturnNoContent() throws Exception {
            // Given
            when(adService.deleteAd(1L, USERNAME)).thenReturn(true);

            // When & Then
            mockMvc.perform(delete("/ads/1").with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(username = USERNAME)
        @DisplayName("Удаление объявления неавторизованным пользователем")
        void deleteAd_WhenNotAuthorized_ShouldReturnNotFound() throws Exception {
            // Given
            when(adService.deleteAd(1L, USERNAME)).thenReturn(false);

            // When & Then
            mockMvc.perform(delete("/ads/1").with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Тесты обновления объявлений")
    class UpdateAdTests {

        @Test
        @WithMockUser(username = USERNAME)
        @DisplayName("Обновление объявления авторизованным пользователем")
        void updateAd_WhenAuthorized_ShouldReturnOk() throws Exception {
            // Given
            AdUpdateRequestDTO updateRequest = new AdUpdateRequestDTO();
            updateRequest.setTitle("Updated Title");
            updateRequest.setPrice(1500);
            updateRequest.setDescription("Updated Description");

            AdFullResponseDTO updatedResponse = new AdFullResponseDTO();
            updatedResponse.setPk(1L);
            updatedResponse.setTitle("Updated Title");

            when(adService.updateAd(eq(1L), any(AdUpdateRequestDTO.class), eq(USERNAME)))
                    .thenReturn(updatedResponse);

            // When & Then
            mockMvc.perform(patch("/ads/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Title"));
        }

        @Test
        @WithMockUser(username = USERNAME)
        @DisplayName("Обновление изображения объявления авторизованным пользователем")
        void updateAdImage_WhenAuthorized_ShouldReturnOk() throws Exception {
            // Given
            MockMultipartFile image = new MockMultipartFile(
                    "image",
                    "new-image.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "new image content".getBytes()
            );

            when(adService.updateAdImage(eq(1L), any(MultipartFile.class), eq(USERNAME)))
                    .thenReturn(true);

            // When & Then - Используем multipart для PATCH запроса
            mockMvc.perform(multipart("/ads/1/image")
                            .file(image)
                            .with(csrf())
                            .with(request -> {
                                request.setMethod("PATCH");
                                return request;
                            })
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isOk());
        }
    }
}