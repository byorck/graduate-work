package ru.skypro.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.skypro.homework.dto.Role;

import java.nio.file.FileStore;

@Entity
@Table(name = "users")
@Schema(description = "Данные пользователей")
public class User {

    @Getter
    @Setter
    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Setter
    @Column(name = "username", columnDefinition = "TEXT")
    private String username;

    @Getter
    @Setter
    @Column(name = "password", columnDefinition = "TEXT")
    private String password;

    @Setter
    @Getter
    @Column(name = "firstName", columnDefinition = "TEXT")
    private String firstName;

    @Getter
    @Setter
    @Column(name = "lastName", columnDefinition = "TEXT")
    private String lastName;

    @Getter
    @Setter
    @Column(name = "phone", columnDefinition = "TEXT")
    private String phone;

    @Getter
    @Setter
    @Column(name = "role", columnDefinition = "TEXT")
    @JsonIgnore
    @Enumerated(EnumType.STRING)
    private Role role;

    public User(Long id, String username, String password, String firstName, String lastName, String phone) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
    }

    public User() {
    }

}
