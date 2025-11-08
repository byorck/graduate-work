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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import ru.skypro.homework.dto.Role;
import ru.skypro.homework.dto.user.UserProfileResponse;
import ru.skypro.homework.dto.user.UserProfileUpdateRequest;
import ru.skypro.homework.entity.Ad;
import ru.skypro.homework.entity.Avatar;
import ru.skypro.homework.entity.User;
import ru.skypro.homework.repository.AvatarRepository;
import ru.skypro.homework.repository.UserRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тестирование UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AvatarRepository avatarRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("Тесты получения профиля пользователя")
    class GetUserProfileTests {

        @Test
        @DisplayName("Получение профиля существующего пользователя")
        void getUserProfile_WhenUserExists_ShouldReturnUserProfileResponse() {
            // Given
            String username = "test@mail.ru";
            User user = new User();
            user.setId(1L);
            user.setUsername(username);
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setPhone("+79998887766");
            user.setRole(Role.USER);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(avatarRepository.findByUser_Username(username)).thenReturn(Optional.empty());

            // When
            UserProfileResponse result = userService.getUserProfile(username);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(username, result.getEmail());
            assertEquals("John", result.getFirstName());
            assertEquals("Doe", result.getLastName());
            assertEquals("+79998887766", result.getPhone());
            assertEquals("USER", result.getRole());
        }

        @Test
        @DisplayName("Получение профиля несуществующего пользователя")
        void getUserProfile_WhenUserNotExists_ShouldReturnNull() {
            // Given
            String username = "nonexistent@mail.ru";
            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            // When
            UserProfileResponse result = userService.getUserProfile(username);

            // Then
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Тесты обновления профиля пользователя")
    class UpdateUserProfileTests {

        @Test
        @DisplayName("Успешное обновление профиля пользователя")
        void updateUserProfile_ShouldUpdateAndReturnProfile() {
            // Given
            String username = "test@mail.ru";
            UserProfileUpdateRequest request = new UserProfileUpdateRequest();
            request.setFirstName("UpdatedJohn");
            request.setLastName("UpdatedDoe");
            request.setPhone("+79998887700");
            request.setRole("ADMIN");

            User user = new User();
            user.setId(1L);
            user.setUsername(username);
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setPhone("+79998887766");
            user.setRole(Role.USER);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            UserProfileResponse result = userService.updateUserProfile(username, request);

            // Then
            assertNotNull(result);
            verify(userRepository, times(1)).save(user);
            assertEquals("UpdatedJohn", user.getFirstName());
            assertEquals("UpdatedDoe", user.getLastName());
            assertEquals("+79998887700", user.getPhone());
            assertEquals(Role.ADMIN, user.getRole());
        }
    }

    @Nested
    @DisplayName("Тесты проверки прав доступа")
    class PermissionTests {

        @Test
        @DisplayName("Проверка прав доступа для администратора")
        void hasPermission_WhenAdmin_ShouldReturnTrue() {
            // Given
            String username = "admin@mail.ru";
            User user = new User();
            user.setUsername(username);
            user.setRole(Role.ADMIN);

            User adUser = new User();
            adUser.setUsername("other@mail.ru");

            Ad ad = new Ad();
            ad.setUser(adUser);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            // When
            boolean result = userService.hasPermission(ad, username);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Проверка прав доступа для автора объявления")
        void hasPermission_WhenAdAuthor_ShouldReturnTrue() {
            // Given
            String username = "author@mail.ru";
            User user = new User();
            user.setUsername(username);
            user.setRole(Role.USER);

            Ad ad = new Ad();
            ad.setUser(user);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            // When
            boolean result = userService.hasPermission(ad, username);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Проверка прав доступа для неавторизованного пользователя")
        void hasPermission_WhenNotAuthorized_ShouldReturnFalse() {
            // Given
            String username = "other@mail.ru";
            User user = new User();
            user.setUsername(username);
            user.setRole(Role.USER);

            User adUser = new User();
            adUser.setUsername("author@mail.ru");

            Ad ad = new Ad();
            ad.setUser(adUser);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            // When
            boolean result = userService.hasPermission(ad, username);

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Тесты загрузки аватара")
    class UploadAvatarTests {

        @Test
        @DisplayName("Успешная загрузка аватара с валидным файлом")
        void uploadAvatar_WithValidFile_ShouldReturnTrue() throws IOException {
            // Given
            String username = "test@mail.ru";

            String fakePath = "/fake/avatars/path";
            ReflectionTestUtils.setField(userService, "avatarsDir", fakePath);

            MockMultipartFile file = new MockMultipartFile(
                    "avatar", "avatar.jpg", "image/jpeg", "test".getBytes()
            );

            User user = new User();
            user.setUsername(username);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(avatarRepository.findByUser_Username(username)).thenReturn(Optional.empty());
            when(avatarRepository.save(any(Avatar.class))).thenAnswer(invocation -> invocation.getArgument(0));

            try (MockedStatic<ImageIO> imageIOMock = mockStatic(ImageIO.class);
                 MockedStatic<Files> filesMock = mockStatic(Files.class)) {

                BufferedImage mockImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
                imageIOMock.when(() -> ImageIO.read(any(InputStream.class))).thenReturn(mockImage);
                imageIOMock.when(() -> ImageIO.write(any(BufferedImage.class), anyString(), any(OutputStream.class)))
                        .thenReturn(true);

                filesMock.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
                filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);
                filesMock.when(() -> Files.newOutputStream(any(Path.class), any())).thenReturn(mock(OutputStream.class));

                // When
                boolean result = userService.uploadAvatar(username, file);

                // Then
                assertTrue(result);
                verify(avatarRepository, times(1)).save(any(Avatar.class));
            }
        }

        @Test
        @DisplayName("Загрузка аватара для несуществующего пользователя")
        void uploadAvatar_WhenUserNotFound_ShouldReturnFalse() throws IOException {
            // Given
            String username = "nonexistent@mail.ru";
            MockMultipartFile file = new MockMultipartFile(
                    "avatar", "avatar.jpg", "image/jpeg", new byte[0]
            );

            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            // When
            boolean result = userService.uploadAvatar(username, file);

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Тесты смены пароля")
    class ChangePasswordTests {

        @Test
        @DisplayName("Успешная смена пароля при правильном текущем пароле")
        void changePassword_WhenCurrentPasswordCorrect_ShouldReturnTrue() {
            // Given
            String username = "test@mail.ru";
            String currentPassword = "oldPassword";
            String newPassword = "newPassword";

            User user = new User();
            user.setUsername(username);
            user.setPassword("encodedOldPassword");

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(currentPassword, user.getPassword())).thenReturn(true);
            when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            boolean result = userService.changePassword(username, currentPassword, newPassword);

            // Then
            assertTrue(result);
            verify(userRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("Смена пароля с неправильным текущим паролем")
        void changePassword_WhenCurrentPasswordIncorrect_ShouldReturnFalse() {
            // Given
            String username = "test@mail.ru";
            String currentPassword = "wrongPassword";
            String newPassword = "newPassword";

            User user = new User();
            user.setUsername(username);
            user.setPassword("encodedOldPassword");

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(currentPassword, user.getPassword())).thenReturn(false);

            // When
            boolean result = userService.changePassword(username, currentPassword, newPassword);

            // Then
            assertFalse(result);
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Тесты поиска аватара")
    class FindAvatarTests {

        @Test
        @DisplayName("Поиск существующего аватара")
        void findAvatar_WhenAvatarExists_ShouldReturnAvatar() {
            // Given
            String username = "test@mail.ru";
            Avatar avatar = new Avatar();

            when(avatarRepository.findByUser_Username(username)).thenReturn(Optional.of(avatar));

            // When
            Avatar result = userService.findAvatar(username);

            // Then
            assertNotNull(result);
            assertEquals(avatar, result);
        }

        @Test
        @DisplayName("Поиск несуществующего аватара")
        void findAvatar_WhenAvatarNotExists_ShouldReturnNull() {
            // Given
            String username = "test@mail.ru";
            when(avatarRepository.findByUser_Username(username)).thenReturn(Optional.empty());

            // When
            Avatar result = userService.findAvatar(username);

            // Then
            assertNull(result);
        }
    }
}