package ru.skypro.homework.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.dto.ad.AdCreateRequestDTO;
import ru.skypro.homework.dto.ad.AdFullResponseDTO;
import ru.skypro.homework.dto.ad.AdShortResponseDTO;
import ru.skypro.homework.dto.ad.AdUpdateRequestDTO;
import ru.skypro.homework.entity.Ad;
import ru.skypro.homework.service.AdService;
import ru.skypro.homework.service.UserService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ads")
@Tag(name = "Объявления")
public class AdController {
    private final AdService adService;
    private final UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(AdController.class);

    public AdController(AdService adService, UserService userService) {
        this.adService = adService;
        this.userService = userService;
    }

    /**
     * Создание нового объявления
     * @param title заголовок объявления
     * @param price цена товара/услуги
     * @param description описание объявления
     * @param imageFile файл изображения для объявления
     * @param authentication данные аутентификации текущего пользователя
     * @return созданное объявление со статусом 201 Created
     * @throws IOException при ошибках загрузки файла изображения
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Добавление объявления")
    public ResponseEntity<AdFullResponseDTO> createAd(
            @RequestParam String title,
            @RequestParam Integer price,
            @RequestParam String description,
            @RequestPart("image") MultipartFile imageFile,
            Authentication authentication) throws IOException {

        logger.info("Creating ad for user: {}", authentication.getName());

        AdCreateRequestDTO adRequest = new AdCreateRequestDTO();
        adRequest.setTitle(title);
        adRequest.setPrice(price);
        adRequest.setDescription(description);

        Ad savedAd = adService.createAd(userService.findUser(authentication.getName()), adRequest, imageFile);
        AdFullResponseDTO response = new AdFullResponseDTO(savedAd);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получение списка всех объявлений
     * @return объект с количеством и списком объявлений
     */
    @GetMapping
    @Operation(summary = "Получение всех объявлений")
    public ResponseEntity<Map<String, Object>> getAllAds() {
        List<AdShortResponseDTO> ads = adService.findAll();
        Map<String, Object> response = Map.of(
                "count", ads.size(),
                "results", ads
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Получение полной информации об объявлении по ID
     * @param id идентификатор объявления
     * @return полная информация об объявлении или 404 если не найдено
     */
    @GetMapping("/{id}")
    @Operation(summary = "Получение информации об объявлении")
    public ResponseEntity<AdFullResponseDTO> getAdById(@PathVariable Long id) {
        AdFullResponseDTO ad = adService.findById(id);
        if (ad == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ad);
    }

    /**
     * Удаление объявления по ID
     * @param id идентификатор объявления
     * @param authentication данные аутентификации для проверки прав доступа
     * @return 204 No Content при успешном удалении, 404 если не найдено, 403 если нет прав
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Удаление объявления")
    public ResponseEntity<Void> deleteAd(@PathVariable Long id, Authentication authentication) {
        Ad ad = adService.getAdEntityById(id);
        if (ad == null) {
            return ResponseEntity.notFound().build();
        }

        if (!userService.hasPermission(ad, authentication)) {
            logger.warn("User {} attempted to delete ad {} without permission", authentication.getName(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean deleted = adService.deleteById(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Обновление информации об объявлении
     * @param id идентификатор объявления
     * @param updateRequest DTO с обновляемыми полями
     * @param authentication данные аутентификации для проверки прав доступа
     * @return 204 No Content при успешном обновлении, 404 если не найдено, 403 если нет прав
     */
    @PatchMapping("/{id}")
    @Operation(summary = "Обновление информации об объявлении")
    public ResponseEntity<Void> updateAd(@PathVariable Long id,
                                         @RequestBody @Valid AdUpdateRequestDTO updateRequest,
                                         Authentication authentication) {
        Ad ad = adService.getAdEntityById(id);
        if (ad == null) {
            return ResponseEntity.notFound().build();
        }

        if (!userService.hasPermission(ad, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean updated = adService.updateAd(id, updateRequest);
        return updated ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Получение объявлений текущего авторизованного пользователя
     * @param auth данные аутентификации текущего пользователя
     * @return объект с количеством и списком объявлений пользователя
     */
    @GetMapping("/me")
    @Operation(summary = "Получение объявлений авторизованного пользователя")
    public ResponseEntity<Map<String, Object>> getMyAds(Authentication auth) {
        String username = auth.getName();
        List<AdShortResponseDTO> ads = adService.findByUsername(username);
        Map<String, Object> response = Map.of(
                "count", ads.size(),
                "results", ads
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Обновление изображения объявления
     * @param id идентификатор объявления
     * @param imageFile новый файл изображения
     * @param auth данные аутентификации для проверки прав доступа
     * @return 204 No Content при успешном обновлении, 404 если не найдено, 403 если нет прав
     * @throws IOException при ошибках загрузки файла изображения
     */
    @PatchMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Обновление картинки объявления")
    public ResponseEntity<Void> updateAdImage(
            @PathVariable Long id,
            @Parameter(description = "Файл изображения")
            @RequestPart("imageFile") MultipartFile imageFile,
            Authentication auth) throws IOException {

        Ad adEntity = adService.getAdEntityById(id);
        if (adEntity == null) {
            return ResponseEntity.notFound().build();
        }

        if (!userService.hasPermission(adEntity, auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        adService.updateAdImage(adEntity, imageFile);
        return ResponseEntity.noContent().build();
    }
}

