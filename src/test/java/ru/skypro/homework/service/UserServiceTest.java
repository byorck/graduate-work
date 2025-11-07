package ru.skypro.homework.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.skypro.homework.dto.Role;
import ru.skypro.homework.dto.user.UserProfileResponse;
import ru.skypro.homework.dto.user.UserProfileUpdateRequest;
import ru.skypro.homework.entity.Ad;
import ru.skypro.homework.entity.Avatar;
import ru.skypro.homework.entity.User;
import ru.skypro.homework.repository.AvatarRepository;
import ru.skypro.homework.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для UserService.
 * Проверяет бизнес-логику работы с пользователями
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тестирование сервиса пользователей")
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
    @DisplayName("Тесты поиска пользователей")
    class FindUserTests {

        @Test
        @DisplayName("Поиск пользователя - когда пользователь существует")
        void findUser_WhenUserExists_ReturnsUser() {
            // Given
            String username = "test@example.com";
            User expectedUser = new User();
            expectedUser.setUsername(username);
            when(userRepository.findByUsername(username)).thenReturn(Optional.of(expectedUser));

            // When
            User result = userRepository.findByUsername(username).orElse(null);

            // Then
            assertNotNull(result);
            assertEquals(username, result.getUsername());
            verify(userRepository).findByUsername(username);
        }

        @Test
        @DisplayName("Поиск пользователя - когда пользователь не существует")
        void findUser_WhenUserNotExists_ReturnsNull() {
            // Given
            String username = "nonexistent@example.com";
            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            // When
            User result = userRepository.findByUsername(username).orElse(null);

            // Then
            assertNull(result);
            verify(userRepository).findByUsername(username);
        }
    }

    @Nested
    @DisplayName("Тесты профиля пользователя")
    class ProfileTests {

        @Test
        @DisplayName("Получение профиля пользователя - когда пользователь существует")
        void getUserProfile_WhenUserExists_ReturnsProfile() {
            // Given
            String username = "test@example.com";
            User user = new User();
            user.setId(1L);
            user.setUsername(username);
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setPhone("+123456789");
            user.setRole(Role.USER);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(avatarRepository.findByUser_Username(username)).thenReturn(Optional.empty());

            // When
            UserProfileResponse result = userService.getUserProfile(username);

            // Then
            assertNotNull(result);
            assertEquals(username, result.getEmail());
            assertEquals("John", result.getFirstName());
            assertEquals("Doe", result.getLastName());
            assertEquals("+123456789", result.getPhone());
            assertEquals("USER", result.getRole());
        }

        @Test
        @DisplayName("Обновление профиля пользователя - когда пользователь существует")
        void updateUserProfile_WhenUserExists_UpdatesProfile() {
            // Given
            String username = "test@example.com";
            User user = new User();
            user.setUsername(username);

            UserProfileUpdateRequest request = new UserProfileUpdateRequest();
            request.setFirstName("Jane");
            request.setLastName("Smith");
            request.setPhone("+987654321");

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);

            // When
            boolean result = userService.updateUserProfile(username, request);

            // Then
            assertTrue(result);
            assertEquals("Jane", user.getFirstName());
            assertEquals("Smith", user.getLastName());
            assertEquals("+987654321", user.getPhone());
            verify(userRepository).save(user);
        }
    }

    @Nested
    @DisplayName("Тесты проверки прав доступа")
    class PermissionTests {

        @Test
        @DisplayName("Проверка прав - когда пользователь администратор")
        void hasPermission_WhenUserIsAdmin_ReturnsTrue() {
            // Given
            String username = "admin@example.com";
            User adminUser = new User();
            adminUser.setUsername(username);
            adminUser.setRole(Role.ADMIN);

            Ad ad = new Ad();
            User adOwner = new User();
            adOwner.setUsername("other@example.com");
            ad.setUser(adOwner);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(adminUser));

            // When
            boolean result = userService.hasPermission(ad, username);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Проверка прав - когда пользователь владелец объявления")
        void hasPermission_WhenUserIsAdOwner_ReturnsTrue() {
            // Given
            String username = "owner@example.com";
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
    }

    @Nested
    @DisplayName("Тесты смены пароля")
    class PasswordTests {

        @Test
        @DisplayName("Смена пароля - когда текущий пароль корректен")
        void changePassword_WhenCurrentPasswordCorrect_UpdatesPassword() {
            // Given
            String username = "test@example.com";
            String currentPassword = "oldPassword";
            String newPassword = "newPassword";

            User user = new User();
            user.setUsername(username);
            user.setPassword("encodedOldPassword");

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(currentPassword, user.getPassword())).thenReturn(true);
            when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");

            // When
            boolean result = userService.changePassword(username, currentPassword, newPassword);

            // Then
            assertTrue(result);
            assertEquals("encodedNewPassword", user.getPassword());
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Смена пароля - когда текущий пароль неверен")
        void changePassword_WhenCurrentPasswordIncorrect_ReturnsFalse() {
            // Given
            String username = "test@example.com";
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
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Тесты работы с аватарами")
    class AvatarTests {

        @Test
        @DisplayName("Поиск аватара - когда аватар существует")
        void findAvatar_WhenAvatarExists_ReturnsAvatar() {
            // Given
            String username = "test@example.com";
            Avatar expectedAvatar = new Avatar();
            when(avatarRepository.findByUser_Username(username)).thenReturn(Optional.of(expectedAvatar));

            // When
            Avatar result = userService.findAvatar(username);

            // Then
            assertNotNull(result);
            assertEquals(expectedAvatar, result);
            verify(avatarRepository).findByUser_Username(username);
        }
    }
}