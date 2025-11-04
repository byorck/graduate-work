package ru.skypro.homework.service;

import jakarta.transaction.Transactional;
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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdService {
    @Value("${path.to.ads.folder}")
    private String adDir;

    private final UserService userService;
    private final AdRepository adRepository;

    public AdService(UserService userService, AdRepository adRepository) {
        this.userService = userService;
        this.adRepository = adRepository;
    }

    // Создание объявления с загрузкой картинки
    public Ad createAd(User user, AdCreateRequestDTO adRequest, MultipartFile imageFile) throws IOException {
        Ad ad = new Ad();
        ad.setUser(user);
        ad.setTitle(adRequest.getTitle());
        ad.setPrice(adRequest.getPrice());
        ad.setDescription(adRequest.getDescription());

        String filename = user.getUsername() + "_" + System.currentTimeMillis() + "." + getExtension(imageFile.getOriginalFilename());
        Path filePath = Path.of(adDir, filename);
        Files.createDirectories(filePath.getParent());
        Files.copy(imageFile.getInputStream(), filePath);

        ad.setFilePath(filePath.toString());
        ad.setFileSize(imageFile.getSize());
        ad.setMediaType(imageFile.getContentType());
        ad.setData(generateAdPreview(filePath));

        return adRepository.save(ad); // Возвращаем сохраненную сущность
    }

    // Получение списка коротких описаний объявлений
    public List<AdShortResponseDTO> findAll() {
        return adRepository.findAll().stream()
                .map(this::toAdShortResponse)
                .collect(Collectors.toList());
    }

    // Получение компактных объявлений пользователя
    public List<AdShortResponseDTO> findByUsername(String username) {
        return adRepository.findByUser_Username(username).stream()
                .map(this::toAdShortResponse)
                .collect(Collectors.toList());
    }

    // Получение подробного объявления по id в виде DTO
    public AdFullResponseDTO findById(Long id) {
        return adRepository.findById(id)
                .map(this::toAdFullResponse)
                .orElse(null);
    }

    // Метод для получения сущности для обновления картинки
    public Ad getAdEntityById(Long id) {
        return adRepository.findById(id).orElse(null);
    }

    // Удаление объявления по id
    public boolean deleteById(Long id) {
        if (adRepository.existsById(id)) {
            adRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Обновление объявления
    public boolean updateAd(Long id, AdUpdateRequestDTO updateRequest) {
        Optional<Ad> optAd = adRepository.findById(id);
        if (optAd.isEmpty()) return false;
        Ad ad = optAd.get();
        ad.setTitle(updateRequest.getTitle());
        ad.setPrice(updateRequest.getPrice());
        ad.setDescription(updateRequest.getDescription());
        adRepository.save(ad);
        return true;
    }

    public void updateAdImage(Ad ad, MultipartFile imageFile) throws IOException {
        String extension = getExtension(Objects.requireNonNull(imageFile.getOriginalFilename()));
        String newFileName = ad.getUser().getUsername() + "_" + System.currentTimeMillis() + "." + extension;

        Path baseDir = Path.of(adDir).toAbsolutePath().normalize();
        Path newFilePath = baseDir.resolve(newFileName).normalize();

        // Более строгая проверка безопасности
        if (!newFilePath.startsWith(baseDir)) {
            throw new SecurityException("Invalid file path: attempted path traversal");
        }

        Files.createDirectories(newFilePath.getParent());
        Files.copy(imageFile.getInputStream(), newFilePath, StandardCopyOption.REPLACE_EXISTING);

        // Удаляем старый файл если он существует и отличается от нового
        if (ad.getFilePath() != null && !ad.getFilePath().equals(newFilePath.toString())) {
            try {
                Files.deleteIfExists(Path.of(ad.getFilePath()));
            } catch (IOException e) {
                // Логируем ошибку, но не прерываем выполнение
                System.err.println("Failed to delete old ad image: " + ad.getFilePath());
            }
        }

        ad.setFilePath(newFilePath.toString());
        ad.setFileSize(imageFile.getSize());
        ad.setMediaType(imageFile.getContentType());
        ad.setData(generateAdPreview(newFilePath));

        adRepository.save(ad);
    }

    // Преобразование в DTO короткого вида
    private AdShortResponseDTO toAdShortResponse(Ad ad) {
        AdShortResponseDTO dto = new AdShortResponseDTO();
        dto.setPk(ad.getId());
        dto.setAuthor(ad.getUser().getId());
        dto.setImage("/ads/" + ad.getId() + "/image");
        dto.setPrice(ad.getPrice());
        dto.setTitle(ad.getTitle());
        return dto;
    }

    // Преобразование в DTO подробного вида
    private AdFullResponseDTO toAdFullResponse(Ad ad) {
        AdFullResponseDTO dto = new AdFullResponseDTO();
        dto.setPk(ad.getId());
        dto.setAuthorFirstName(ad.getUser().getFirstName());
        dto.setAuthorLastName(ad.getUser().getLastName());
        dto.setEmail(ad.getUser().getUsername());
        dto.setPhone(ad.getUser().getPhone());
        dto.setImage("/ads/" + ad.getId() + "/image");
        dto.setPrice(ad.getPrice());
        dto.setTitle(ad.getTitle());
        dto.setDescription(ad.getDescription());
        return dto;
    }

    private String getExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private byte[] generateAdPreview(Path filePath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(filePath);
             BufferedInputStream bis = new BufferedInputStream(inputStream, 1024);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            BufferedImage image = ImageIO.read(bis);
            if (image == null) {
                throw new IOException("Unsupported image format");
            }

            int width = 100;
            int height = image.getHeight() * width / image.getWidth(); // сохраняем пропорции
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
