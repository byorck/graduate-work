package ru.skypro.homework.dto.user;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class UserProfileUpdateRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String role;
}
