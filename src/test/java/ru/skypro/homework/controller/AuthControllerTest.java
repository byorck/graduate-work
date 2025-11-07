package ru.skypro.homework.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.skypro.homework.config.TestConfig;
import ru.skypro.homework.config.WebSecurityConfig;
import ru.skypro.homework.service.AuthService;
import ru.skypro.homework.service.CustomUserDetailsService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тестовый класс для AuthController.
 * Проверяет endpoints аутентификации и регистрации
 */
@WebMvcTest(AuthController.class)
@Import({WebSecurityConfig.class, TestConfig.class})
@DisplayName("Тестирование контроллера авторизации")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @Nested
    @DisplayName("Тесты аутентификации")
    class LoginTests {

        @Test
        @DisplayName("Вход в систему - с валидными учетными данными")
        void login_WithValidCredentials_ShouldReturnOk() throws Exception {
            // Given
            UserDetails userDetails = User.withUsername("user@mail.com")
                    .password("password")
                    .roles("USER")
                    .build();

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    "password",
                    userDetails.getAuthorities()
            );

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);

            String jsonContent = "{\"username\": \"user@mail.com\", \"password\": \"password\"}";

            // When & Then
            mockMvc.perform(post("/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonContent))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.username").value("user@mail.com"))
                    .andExpect(jsonPath("$.message").value("Successfully logged in"));
        }

        @Test
        @DisplayName("Вход в систему - с невалидными учетными данными")
        void login_WithInvalidCredentials_ShouldReturnUnauthorized() throws Exception {
            // Given
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            String jsonContent = "{\"username\": \"user@mail.com\", \"password\": \"wrongpassword\"}";

            // When & Then
            mockMvc.perform(post("/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonContent))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }
    }

    @Nested
    @DisplayName("Тесты регистрации")
    class RegisterTests {

        @Test
        @DisplayName("Регистрация - с валидными данными")
        void register_WithValidData_ShouldReturnCreated() throws Exception {
            // Given
            when(authService.register(any())).thenReturn(true);

            String jsonContent = "{\"username\": \"newuser@mail.com\", \"password\": \"password\", \"firstName\": \"John\", \"lastName\": \"Doe\", \"phone\": \"+123456789\"}";

            // When & Then
            mockMvc.perform(post("/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonContent))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Регистрация - с существующим именем пользователя")
        void register_WithExistingUsername_ShouldReturnBadRequest() throws Exception {
            // Given
            when(authService.register(any())).thenReturn(false);

            String jsonContent = "{\"username\": \"existing@mail.com\", \"password\": \"password\", \"firstName\": \"John\", \"lastName\": \"Doe\", \"phone\": \"+123456789\"}";

            // When & Then
            mockMvc.perform(post("/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonContent))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("Выход из системы - должен завершить сессию")
    void logout_ShouldReturnOk() throws Exception {
        // When & Then
        mockMvc.perform(post("/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("JSESSIONID"))
                .andExpect(header().exists("Set-Cookie"));
    }
}