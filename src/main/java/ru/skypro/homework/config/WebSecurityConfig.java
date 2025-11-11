package ru.skypro.homework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import ru.skypro.homework.service.CustomUserDetailsService;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Конфигурация безопасности приложения.
 * Настраивает аутентификацию, авторизацию и защиту endpoints.
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private static final String[] AUTH_WHITELIST = {
            "/swagger-resources/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/webjars/**",
            "/login",
            "/register",
            "/logout",
            "/ads/*/image",
            "/users/*/avatar",
            "/images/**"
    };

    private final CustomUserDetailsService customUserDetailsService;

    public WebSecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    /**
     * Настройка цепочки фильтров безопасности.
     *
     * @param http объект для настройки безопасности HTTP
     * @return сконфигурированная цепочка фильтров
     * @throws Exception если произошла ошибка конфигурации
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .expiredUrl("/login?expired")
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AUTH_WHITELIST).permitAll() // публичный доступ
                        .requestMatchers("/ads/**", "/users/**").authenticated()
                        .anyRequest().authenticated()
                )
                .userDetailsService(customUserDetailsService)
                .cors(withDefaults())
                .httpBasic(withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(200);

                            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                        })
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }
}