package ru.skypro.homework.dto.ad;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class AdShortResponseDTO {
    private Long pk;
    private Long author;
    private String image;
    private Integer price;
    private String title;
}
