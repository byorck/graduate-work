package ru.skypro.homework.dto.comment;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentDTO {
    private Long id;
    private String text;
    private Long author;
    private String authorImage;
    private String authorFirstName;
    private LocalDateTime createdAt;
}
