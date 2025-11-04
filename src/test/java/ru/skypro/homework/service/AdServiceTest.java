package ru.skypro.homework.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.skypro.homework.dto.ad.AdFullResponseDTO;
import ru.skypro.homework.dto.ad.AdShortResponseDTO;
import ru.skypro.homework.entity.Ad;
import ru.skypro.homework.entity.User;
import ru.skypro.homework.repository.AdRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для AdService.
 * Проверяет бизнес-логику работы с объявлениями
 */
@ExtendWith(MockitoExtension.class)
class AdServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private AdRepository adRepository;

    @InjectMocks
    private AdService adService;

    @Nested
    @DisplayName("Тесты получения объявлений")
    class GetAdsTests {

        @Test
        @DisplayName("Получение всех объявлений - когда объявления существуют")
        void getAllAds_WhenAdsExist_ReturnsAdsMap() {
            // Given
            User user = new User();
            user.setId(1L);

            Ad ad1 = new Ad();
            ad1.setId(1L);
            ad1.setTitle("Ad 1");
            ad1.setPrice(100);
            ad1.setUser(user);

            Ad ad2 = new Ad();
            ad2.setId(2L);
            ad2.setTitle("Ad 2");
            ad2.setPrice(200);
            ad2.setUser(user);

            when(adRepository.findAll()).thenReturn(List.of(ad1, ad2));

            // When
            Map<String, Object> result = adService.getAllAds();

            // Then
            assertNotNull(result);
            assertEquals(2, result.get("count"));

            @SuppressWarnings("unchecked")
            List<AdShortResponseDTO> results = (List<AdShortResponseDTO>) result.get("results");
            assertEquals(2, results.size());
            assertEquals("Ad 1", results.get(0).getTitle());
            assertEquals("Ad 2", results.get(1).getTitle());
        }

        @Test
        @DisplayName("Получение объявления по ID - когда объявление существует")
        void getAdById_WhenAdExists_ReturnsAd() {
            // Given
            Long adId = 1L;
            User user = new User();
            user.setId(1L);
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setUsername("john@example.com");
            user.setPhone("+123456789");

            Ad ad = new Ad();
            ad.setId(adId);
            ad.setTitle("Test Ad");
            ad.setPrice(100);
            ad.setDescription("Test Description");
            ad.setUser(user);

            when(adRepository.findById(adId)).thenReturn(Optional.of(ad));

            // When
            AdFullResponseDTO result = adService.getAdById(adId);

            // Then
            assertNotNull(result);
            assertEquals(adId, result.getPk());
            assertEquals("Test Ad", result.getTitle());
            assertEquals(100, result.getPrice());
            assertEquals("Test Description", result.getDescription());
            assertEquals("John", result.getAuthorFirstName());
            assertEquals("Doe", result.getAuthorLastName());
        }

        @Test
        @DisplayName("Получение объявлений пользователя - когда объявления существуют")
        void getUserAds_WhenUserHasAds_ReturnsAds() {
            // Given
            String username = "test@example.com";
            User user = new User();
            user.setId(1L);

            Ad ad1 = new Ad();
            ad1.setId(1L);
            ad1.setTitle("User Ad 1");
            ad1.setPrice(100);
            ad1.setUser(user);

            Ad ad2 = new Ad();
            ad2.setId(2L);
            ad2.setTitle("User Ad 2");
            ad2.setPrice(200);
            ad2.setUser(user);

            when(adRepository.findByUser_Username(username)).thenReturn(List.of(ad1, ad2));

            // When
            Map<String, Object> result = adService.getUserAds(username);

            // Then
            assertNotNull(result);
            assertEquals(2, result.get("count"));

            @SuppressWarnings("unchecked")
            List<AdShortResponseDTO> results = (List<AdShortResponseDTO>) result.get("results");
            assertEquals(2, results.size());
            assertEquals("User Ad 1", results.get(0).getTitle());
            assertEquals("User Ad 2", results.get(1).getTitle());
        }
    }

    @Nested
    @DisplayName("Тесты управления объявлениями")
    class ManageAdsTests {

        @Test
        @DisplayName("Удаление объявления - когда пользователь имеет права")
        void deleteAd_WhenUserHasPermission_DeletesAd() {
            // Given
            Long adId = 1L;
            String username = "owner@example.com";

            User user = new User();
            user.setUsername(username);

            Ad ad = new Ad();
            ad.setId(adId);
            ad.setUser(user);

            when(adRepository.findById(adId)).thenReturn(Optional.of(ad));
            when(userService.hasPermission(ad, username)).thenReturn(true);

            // When
            boolean result = adService.deleteAd(adId, username);

            // Then
            assertTrue(result);
            verify(adRepository).deleteById(adId);
        }

        @Test
        @DisplayName("Удаление объявления - когда пользователь не имеет прав")
        void deleteAd_WhenUserNoPermission_ReturnsFalse() {
            // Given
            Long adId = 1L;
            String username = "other@example.com";

            User owner = new User();
            owner.setUsername("owner@example.com");

            Ad ad = new Ad();
            ad.setId(adId);
            ad.setUser(owner);

            when(adRepository.findById(adId)).thenReturn(Optional.of(ad));
            when(userService.hasPermission(ad, username)).thenReturn(false);

            // When
            boolean result = adService.deleteAd(adId, username);

            // Then
            assertFalse(result);
            verify(adRepository, never()).deleteById(any());
        }
    }
}