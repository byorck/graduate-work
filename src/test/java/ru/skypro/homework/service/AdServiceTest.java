package ru.skypro.homework.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import ru.skypro.homework.dto.ad.AdFullResponseDTO;
import ru.skypro.homework.dto.ad.AdUpdateRequestDTO;
import ru.skypro.homework.entity.Ad;
import ru.skypro.homework.entity.User;
import ru.skypro.homework.repository.AdRepository;
import ru.skypro.homework.repository.UserRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тестирование AdService")
class AdServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private AdRepository adRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdService adService;

    @Nested
    @DisplayName("Тесты создания объявлений")
    class CreateAdTests {

        @Test
        @DisplayName("Создание объявления с валидными данными")
        void createAd_WithValidData_ShouldReturnAdFullResponseDTO() throws IOException {
            // Given
            String username = "test@mail.ru";
            String title = "Test Ad";
            Integer price = 1000;
            String description = "Test Description";

            MockMultipartFile imageFile = new MockMultipartFile(
                    "image",
                    "test.jpg",
                    "image/jpeg",
                    "test image content".getBytes()
            );

            User user = new User();
            user.setUsername(username);
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setPhone("+79998887766");

            ReflectionTestUtils.setField(adService, "adDir", "/tmp/test");

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(adRepository.save(any(Ad.class))).thenAnswer(invocation -> {
                Ad ad = invocation.getArgument(0);
                ad.setId(1L);
                ad.setFilePath("/tmp/test/test_file.jpg");
                return ad;
            });

            try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
                filesMock.when(() -> Files.createDirectories(any(Path.class))).thenReturn(Paths.get("/tmp/test"));
                filesMock.when(() -> Files.copy(any(InputStream.class), any(Path.class), any(CopyOption.class)))
                        .thenReturn(1L);

                try (MockedStatic<ImageIO> imageIOMock = mockStatic(ImageIO.class)) {
                    BufferedImage mockImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
                    imageIOMock.when(() -> ImageIO.read(any(InputStream.class))).thenReturn(mockImage);
                    imageIOMock.when(() -> ImageIO.write(any(BufferedImage.class), anyString(), any(OutputStream.class)))
                            .thenReturn(true);

                    // When
                    AdFullResponseDTO result = adService.createAd(username, title, price, description, imageFile);

                    // Then
                    assertNotNull(result);
                    assertEquals(1L, result.getPk());
                    assertEquals(title, result.getTitle());
                    assertEquals(price, result.getPrice());
                    assertEquals(description, result.getDescription());
                }
            }
        }

        @Test
        @DisplayName("Создание объявления несуществующим пользователем")
        void createAd_WithNonExistentUser_ShouldReturnNull() throws IOException {
            // Given
            String username = "nonexistent@mail.ru";
            MockMultipartFile imageFile = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", "test".getBytes()
            );

            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            // When
            AdFullResponseDTO result = adService.createAd(username, "Title", 1000, "Description", imageFile);

            // Then
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Тесты получения объявлений")
    class GetAdsTests {

        @Test
        @DisplayName("Получение всех объявлений")
        void getAllAds_ShouldReturnMapWithCountAndResults() {
            // Given
            User user = new User();
            user.setId(1L);

            Ad ad1 = new Ad();
            ad1.setId(1L);
            ad1.setTitle("Ad 1");
            ad1.setPrice(1000);
            ad1.setUser(user);

            Ad ad2 = new Ad();
            ad2.setId(2L);
            ad2.setTitle("Ad 2");
            ad2.setPrice(2000);
            ad2.setUser(user);

            when(adRepository.findAll()).thenReturn(List.of(ad1, ad2));

            // When
            Map<String, Object> result = adService.getAllAds();

            // Then
            assertNotNull(result);
            assertEquals(2, result.get("count"));
            assertTrue(result.containsKey("results"));
        }

        @Test
        @DisplayName("Получение объявлений пользователя")
        void getUserAds_ShouldReturnUserAds() {
            // Given
            String username = "test@mail.ru";
            User user = new User();
            user.setId(1L);
            user.setUsername(username);

            Ad ad = new Ad();
            ad.setId(1L);
            ad.setTitle("User Ad");
            ad.setPrice(1000);
            ad.setUser(user);

            when(adRepository.findByUser_Username(username)).thenReturn(List.of(ad));

            // When
            Map<String, Object> result = adService.getUserAds(username);

            // Then
            assertNotNull(result);
            assertEquals(1, result.get("count"));
        }

        @Test
        @DisplayName("Получение объявления по ID")
        void getAdById_WhenAdExists_ShouldReturnAdFullResponseDTO() {
            // Given
            Long adId = 1L;
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setUsername("test@mail.ru");
            user.setPhone("+79998887766");

            Ad ad = new Ad();
            ad.setId(adId);
            ad.setTitle("Test Ad");
            ad.setPrice(1000);
            ad.setDescription("Test Description");
            ad.setUser(user);

            when(adRepository.findById(adId)).thenReturn(Optional.of(ad));

            // When
            AdFullResponseDTO result = adService.getAdById(adId);

            // Then
            assertNotNull(result);
            assertEquals(adId, result.getPk());
            assertEquals("Test Ad", result.getTitle());
        }

        @Test
        @DisplayName("Получение несуществующего объявления по ID")
        void getAdById_WhenAdNotExists_ShouldReturnNull() {
            // Given
            Long adId = 999L;
            when(adRepository.findById(adId)).thenReturn(Optional.empty());

            // When
            AdFullResponseDTO result = adService.getAdById(adId);

            // Then
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Тесты удаления объявлений")
    class DeleteAdTests {

        @Test
        @DisplayName("Удаление объявления авторизованным пользователем")
        void deleteAd_WhenAuthorized_ShouldReturnTrue() {
            // Given
            Long adId = 1L;
            String username = "test@mail.ru";

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
            verify(adRepository, times(1)).deleteById(adId);
        }

        @Test
        @DisplayName("Удаление объявления неавторизованным пользователем")
        void deleteAd_WhenNotAuthorized_ShouldReturnFalse() {
            // Given
            Long adId = 1L;
            String username = "other@mail.ru";

            User user = new User();
            user.setUsername("test@mail.ru");

            Ad ad = new Ad();
            ad.setId(adId);
            ad.setUser(user);

            when(adRepository.findById(adId)).thenReturn(Optional.of(ad));
            when(userService.hasPermission(ad, username)).thenReturn(false);

            // When
            boolean result = adService.deleteAd(adId, username);

            // Then
            assertFalse(result);
            verify(adRepository, never()).deleteById(adId);
        }
    }

    @Nested
    @DisplayName("Тесты обновления объявлений")
    class UpdateAdTests {

        @Test
        @DisplayName("Обновление объявления авторизованным пользователем")
        void updateAd_WhenAuthorized_ShouldReturnUpdatedAd() {
            // Given
            Long adId = 1L;
            String username = "test@mail.ru";

            AdUpdateRequestDTO updateRequest = new AdUpdateRequestDTO();
            updateRequest.setTitle("Updated Title");
            updateRequest.setPrice(1500);
            updateRequest.setDescription("Updated Description");

            User user = new User();
            user.setUsername(username);
            user.setFirstName("John");
            user.setLastName("Doe");

            Ad ad = new Ad();
            ad.setId(adId);
            ad.setTitle("Original Title");
            ad.setPrice(1000);
            ad.setDescription("Original Description");
            ad.setUser(user);

            when(adRepository.findById(adId)).thenReturn(Optional.of(ad));
            when(userService.hasPermission(ad, username)).thenReturn(true);
            when(adRepository.save(any(Ad.class))).thenReturn(ad);

            // When
            AdFullResponseDTO result = adService.updateAd(adId, updateRequest, username);

            // Then
            assertNotNull(result);
            assertEquals("Updated Title", result.getTitle());
            assertEquals(1500, result.getPrice());
            assertEquals("Updated Description", result.getDescription());
        }

        @Test
        @DisplayName("Обновление объявления неавторизованным пользователем")
        void updateAd_WhenNotAuthorized_ShouldReturnNull() {
            // Given
            Long adId = 1L;
            String username = "other@mail.ru";

            AdUpdateRequestDTO updateRequest = new AdUpdateRequestDTO();
            updateRequest.setTitle("Updated Title");

            User user = new User();
            user.setUsername("test@mail.ru");

            Ad ad = new Ad();
            ad.setId(adId);
            ad.setUser(user);

            when(adRepository.findById(adId)).thenReturn(Optional.of(ad));
            when(userService.hasPermission(ad, username)).thenReturn(false);

            // When
            AdFullResponseDTO result = adService.updateAd(adId, updateRequest, username);

            // Then
            assertNull(result);
            verify(adRepository, never()).save(any(Ad.class));
        }
    }
}