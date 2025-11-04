package ru.skypro.homework.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "avatars")
@Schema(description = "Аватары пользователей")
public class Avatar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filePath;
    private long fileSize;
    private String mediaType;

    @Column(columnDefinition = "bytea")
    private byte[] data;

    @OneToOne
    private User user;

}
