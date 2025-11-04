package ru.skypro.homework.dto.ad;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Data
public class AdCreateRequestDTO {
    private String title;
    private Integer price;
    private String description;
}
