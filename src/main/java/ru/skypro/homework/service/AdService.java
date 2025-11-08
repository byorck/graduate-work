package ru.skypro.homework.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.dto.ad.AdCreateRequestDTO;
import ru.skypro.homework.dto.ad.AdFullResponseDTO;
import ru.skypro.homework.dto.ad.AdShortResponseDTO;
import ru.skypro.homework.dto.ad.AdUpdateRequestDTO;
import ru.skypro.homework.entity.Ad;
import ru.skypro.homework.entity.User;
import ru.skypro.homework.repository.AdRepository;
import ru.skypro.homework.repository.UserRepository;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Сервис для работы с объявлениями.
 * Обрабатывает бизнес-логику создания, получения, обновления и удаления объявлений
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AdService {
    @Value("${path.to.ads.folder}")
    private String adDir;

    private final UserService userService;
    private final AdRepository adRepository;
    private final UserRepository userRepository;

    /**
     * Создание нового объявления
     *
     * @param username    имя пользователя-автора
     * @param title       заголовок объявления
     * @param price       цена товара
     * @param description описание товара
     * @param imageFile   файл изображения
     * @return DTO созданного объявления или null при ошибке
     * @throws IOException при ошибках работы с файловой системой
     */
    public AdFullResponseDTO createAd(String username, String title, Integer price, String description, MultipartFile imageFile) throws IOException {
        log.debug("Creating ad for user: {}", username);

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            log.warn("User {} not found for ad creation", username);
            return null;
        }

        AdCreateRequestDTO adRequest = new AdCreateRequestDTO();
        adRequest.setTitle(title);
        adRequest.setPrice(price);
        adRequest.setDescription(description);

        Ad ad = new Ad();
        ad.setUser(user);
        ad.setTitle(adRequest.getTitle());
        ad.setPrice(adRequest.getPrice());
        ad.setDescription(adRequest.getDescription());

        String filename = user.getUsername() + "_" + System.currentTimeMillis() + "." + getExtension(Objects.requireNonNull(imageFile.getOriginalFilename()));
        Path filePath = Path.of(adDir, filename);
        Files.createDirectories(filePath.getParent());
        Files.copy(imageFile.getInputStream(), filePath);

        ad.setFilePath(filePath.toString());
        ad.setFileSize(imageFile.getSize());
        ad.setMediaType(imageFile.getContentType());
        ad.setData(generateAdPreview(filePath));

        Ad savedAd = adRepository.save(ad);
        log.info("Ad {} created successfully by user {}", savedAd.getId(), username);
        return new AdFullResponseDTO(savedAd);
    }

    /**
     * Получение всех объявлений
     *
     * @return карта с количеством и списком объявлений
     */
    public Map<String, Object> getAllAds() {
        List<AdShortResponseDTO> ads = adRepository.findAll().stream()
                .map(this::toAdShortResponse)
                .collect(Collectors.toList());

        return Map.of(
                "count", ads.size(),
                "results", ads
        );
    }

    /**
     * Получение объявлений конкретного пользователя
     *
     * @param username имя пользователя
     * @return карта с количеством и списком объявлений пользователя
     */
    public Map<String, Object> getUserAds(String username) {
        List<AdShortResponseDTO> ads = adRepository.findByUser_Username(username).stream()
                .map(this::toAdShortResponse)
                .collect(Collectors.toList());

        return Map.of(
                "count", ads.size(),
                "results", ads
        );
    }

    /**
     * Получение объявления по идентификатору
     *
     * @param id идентификатор объявления
     * @return DTO объявления или null если не найдено
     */
    public AdFullResponseDTO getAdById(Long id) {
        return adRepository.findById(id)
                .map(AdFullResponseDTO::new)
                .orElse(null);
    }

    /**
     * Удаление объявления с проверкой прав доступа
     *
     * @param id       идентификатор объявления
     * @param username имя пользователя, выполняющего операцию
     * @return true если удаление успешно, false если нет прав или объявление не найдено
     */
    public boolean deleteAd(Long id, String username) {
        log.debug("Deleting ad {} by user {}", id, username);

        Ad ad = adRepository.findById(id).orElse(null);
        if (ad == null) {
            log.warn("Ad {} not found for deletion", id);
            return false;
        }

        if (!userService.hasPermission(ad, username)) {
            log.warn("User {} attempted to delete ad {} without permission", username, id);
            return false;
        }

        adRepository.deleteById(id);
        log.info("Ad {} deleted successfully by user {}", id, username);
        return true;
    }

    /**
     * Обновление информации об объявлении с проверкой прав доступа
     *
     * @param id            идентификатор объявления
     * @param updateRequest DTO с обновленными данными
     * @param username      имя пользователя, выполняющего операцию
     * @return true если обновление успешно, false если нет прав или объявление не найдено
     */
    public AdFullResponseDTO updateAd(Long id, AdUpdateRequestDTO updateRequest, String username) {
        log.debug("Updating ad {} by user {}", id, username);

        Optional<Ad> optAd = adRepository.findById(id);
        if (optAd.isEmpty()) {
            return null;
        }

        Ad ad = optAd.get();
        if (!userService.hasPermission(ad, username)) {
            log.warn("User {} attempted to update ad {} without permission", username, id);
            return null;
        }

        ad.setTitle(updateRequest.getTitle());
        ad.setPrice(updateRequest.getPrice());
        ad.setDescription(updateRequest.getDescription());
        Ad savedAd = adRepository.save(ad);

        log.info("Ad {} updated successfully by user {}", id, username);
        return new AdFullResponseDTO(savedAd);
    }

    /**
     * Обновление изображения объявления с проверкой прав доступа
     *
     * @param id        идентификатор объявления
     * @param imageFile новый файл изображения
     * @param username  имя пользователя, выполняющего операцию
     * @return true если обновление успешно, false если нет прав или объявление не найдено
     * @throws IOException при ошибках работы с файловой системой
     */
    public boolean updateAdImage(Long id, MultipartFile imageFile, String username) throws IOException {
        log.debug("Updating ad image for ad {} by user {}", id, username);

        Ad ad = adRepository.findById(id).orElse(null);
        if (ad == null) {
            return false;
        }

        if (!userService.hasPermission(ad, username)) {
            log.warn("User {} attempted to update ad image {} without permission", username, id);
            return false;
        }

        updateAdImageInternal(ad, imageFile);
        return true;
    }

    /**
     * Внутренний метод для обновления изображения объявления
     *
     * @param ad        сущность объявления
     * @param imageFile новый файл изображения
     * @throws IOException при ошибках работы с файловой системой
     */
    private void updateAdImageInternal(Ad ad, MultipartFile imageFile) throws IOException {
        String extension = getExtension(Objects.requireNonNull(imageFile.getOriginalFilename()));
        String newFileName = ad.getUser().getUsername() + "_" + System.currentTimeMillis() + "." + extension;

        Path baseDir = Path.of(adDir).toAbsolutePath().normalize();
        Path newFilePath = baseDir.resolve(newFileName).normalize();

        if (!newFilePath.startsWith(baseDir)) {
            throw new SecurityException("Invalid file path: attempted path traversal");
        }

        Files.createDirectories(newFilePath.getParent());
        Files.copy(imageFile.getInputStream(), newFilePath, StandardCopyOption.REPLACE_EXISTING);

        if (ad.getFilePath() != null && !ad.getFilePath().equals(newFilePath.toString())) {
            try {
                Files.deleteIfExists(Path.of(ad.getFilePath()));
            } catch (IOException e) {
                log.error("Failed to delete old ad image: {}", ad.getFilePath(), e);
            }
        }

        ad.setFilePath(newFilePath.toString());
        ad.setFileSize(imageFile.getSize());
        ad.setMediaType(imageFile.getContentType());
        ad.setData(generateAdPreview(newFilePath));

        adRepository.save(ad);
        log.info("Ad image updated for ad {}", ad.getId());
    }

    /**
     * Преобразование сущности в DTO для краткого представления
     */
    private AdShortResponseDTO toAdShortResponse(Ad ad) {
        AdShortResponseDTO dto = new AdShortResponseDTO();
        dto.setPk(ad.getId());
        dto.setAuthor(ad.getUser().getId());
        dto.setImage("/ads/" + ad.getId() + "/image");
        dto.setPrice(ad.getPrice());
        dto.setTitle(ad.getTitle());
        return dto;
    }

    /**
     * Получение расширения файла из имени
     */
    private String getExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    /**
     * Генерация превью изображения для объявления
     *
     * @param filePath путь к исходному файлу изображения
     * @return массив байтов превью изображения
     * @throws IOException при ошибках чтения/записи изображения
     */
    private byte[] generateAdPreview(Path filePath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(filePath);
             BufferedInputStream bis = new BufferedInputStream(inputStream, 1024);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            BufferedImage image = ImageIO.read(bis);
            if (image == null) {
                throw new IOException("Unsupported image format");
            }

            int width = 100;
            int height = image.getHeight() * width / image.getWidth();
            BufferedImage preview = new BufferedImage(width, height, image.getType());
            Graphics2D graphics = preview.createGraphics();
            graphics.drawImage(image, 0, 0, width, height, null);
            graphics.dispose();

            String ext = getExtension(filePath.getFileName().toString());
            ImageIO.write(preview, ext, baos);
            return baos.toByteArray();
        }
    }
}