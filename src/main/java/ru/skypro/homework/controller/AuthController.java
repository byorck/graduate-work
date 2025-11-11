package ru.skypro.homework.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.skypro.homework.dto.Login;
import ru.skypro.homework.dto.Register;
import ru.skypro.homework.service.AuthService;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@CrossOrigin(value = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;


    /**
     * Аутентификация пользователя в системе
     *
     * @param login DTO с данными для входа (логин и пароль)
     * @return 200 OK при успешной аутентификации, 401 Unauthorized при ошибке
     */
    @Tag(name = "Авторизация")
    @Operation(summary = "Авторизация пользователя")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Login login, HttpServletRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            login.getUsername(),
                            login.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }

            HttpSession newSession = request.getSession(true);
            newSession.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            newSession.setMaxInactiveInterval(30 * 60);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("username", login.getUsername());
            response.put("sessionId", newSession.getId());
            response.put("message", "Successfully logged in");

            log.info("User {} successfully authenticated. New session: {}",
                    login.getUsername(), newSession.getId());

            return ResponseEntity.ok()
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .body(response);

        } catch (Exception e) {
            log.warn("Failed authentication attempt for user: {}", login.getUsername());

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Invalid username or password");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .body(errorResponse);
        }
    }

    /**
     * Регистрация нового пользователя в системе
     *
     * @param register DTO с данными для регистрации
     * @return 201 Created при успешной регистрации, 400 Bad Request при ошибке
     */
    @Tag(name = "Регистрация")
    @Operation(summary = "Регистрация пользователя")
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Register register) {
        if (authService.register(register)) {
            log.info("User {} successfully registered", register.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } else {
            log.warn("Failed registration attempt for user: {}", register.getUsername());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Выход пользователя из системы
     *
     * @return 200 OK при успешном выходе
     */
    @Tag(name = "Авторизация")
    @Operation(summary = "Выход пользователя")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "unknown";

            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }

            SecurityContextHolder.clearContext();

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Successfully logged out");

            log.info("User {} successfully logged out. Session invalidated.", username);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during logout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}