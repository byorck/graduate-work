package ru.skypro.homework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import ru.skypro.homework.dto.Role;
import ru.skypro.homework.service.CustomUserDetailsService;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class WebSecurityConfig {

    private static final String[] AUTH_WHITELIST = {
            "/swagger-resources/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/webjars/**",
            "/login",
            "/register"
    };

    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordEncoder passwordEncoder;

    public WebSecurityConfig(CustomUserDetailsService customUserDetailsService,
                             PasswordEncoder passwordEncoder) {
        this.customUserDetailsService = customUserDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AUTH_WHITELIST).permitAll()
                        .requestMatchers("/ads/**", "/users/**").authenticated()
                )
                .userDetailsService(customUserDetailsService)
                .cors(withDefaults())
                .httpBasic(withDefaults());
        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.builder()
                .username("user@gmail.com")
                .password("password")
                .passwordEncoder(passwordEncoder::encode)
                .roles(Role.USER.name())
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}
