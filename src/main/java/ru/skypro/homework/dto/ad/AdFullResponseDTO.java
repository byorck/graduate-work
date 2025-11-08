package ru.skypro.homework.dto.ad;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import ru.skypro.homework.entity.Ad;

@Setter
@Getter
@Data
public class AdFullResponseDTO {
    private Long pk;
    private String authorFirstName;
    private String authorLastName;
    private String email;
    private String phone;
    private String image;
    private Integer price;
    private String title;
    private String description;

    public AdFullResponseDTO(Ad ad) {
        this.pk = ad.getId();
        this.authorFirstName = ad.getUser().getFirstName();
        this.authorLastName = ad.getUser().getLastName();
        this.email = ad.getUser().getUsername();
        this.phone = ad.getUser().getPhone();
        this.image = "/ads/" + ad.getId() + "/image";
        this.price = ad.getPrice();
        this.title = ad.getTitle();
        this.description = ad.getDescription();
    }

    public AdFullResponseDTO() {
    }
}
