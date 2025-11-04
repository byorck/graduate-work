package ru.skypro.homework.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "ads")
@Schema(description = "Объявления пользователей")
public class Ad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "price", columnDefinition = "INTEGER")
    private Integer price;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_path", columnDefinition = "TEXT")
    private String filePath;

    @Column(name = "file_size", columnDefinition = "BIGINT")
    private long fileSize;

    @Column(name = "media_type", columnDefinition = "TEXT")
    private String mediaType;

    @Column(columnDefinition = "bytea")
    private byte[] data;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    public Ad(Long id, String title, Integer price, String description, String filePath, long fileSize, String mediaType, byte[] data) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.description = description;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.mediaType = mediaType;
        this.data = data;
    }

    public Ad() {
    }
}