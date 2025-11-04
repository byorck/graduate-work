package ru.skypro.homework.dto.ad;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class AdUpdateRequestDTO {
    private String title;
    private Integer price;
    private String description;
}
