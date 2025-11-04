package ru.skypro.homework.service;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.entity.Avatar;
import ru.skypro.homework.entity.User;
import ru.skypro.homework.repository.AvatarRepository;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

@Service
@Transactional
public class AvatarService {

    @Value("${path.to.avatars.folder}")
    private String avatarsDir;

    private final AvatarRepository avatarRepository;

    public AvatarService(AvatarRepository avatarRepository) {
        this.avatarRepository = avatarRepository;
    }

    private static final Logger logger = LoggerFactory.getLogger(AvatarService.class);

    public void uploadAvatar(User user, MultipartFile file) throws IOException {
        Path filePath = Path.of(avatarsDir, user.getUsername() + "." + getExtension(file.getOriginalFilename()));
        Files.createDirectories(filePath.getParent());
        Files.deleteIfExists(filePath);

        try (InputStream inputStream = file.getInputStream();
             OutputStream outputStream = Files.newOutputStream(filePath, CREATE_NEW);
             BufferedInputStream bis = new BufferedInputStream(inputStream, 1024);
             BufferedOutputStream bos = new BufferedOutputStream(outputStream, 1024)) {
            bis.transferTo(bos);
        }

        Avatar avatar = findAvatar(user.getUsername());
        if (avatar == null) {
            avatar = new Avatar();
        }
        avatar.setUser(user);
        avatar.setFilePath(filePath.toString());
        avatar.setFileSize(file.getSize());
        avatar.setMediaType(file.getContentType());
        avatar.setData(generateAvatarPreview(filePath));

        avatarRepository.save(avatar);
    }

    public Avatar findAvatar(String username) {
        logger.info("Was invoked method for find avatar by user id");
        return avatarRepository.findByUser_Username(username).orElse(null);
    }

    private byte[] generateAvatarPreview(Path filePath) throws IOException { // создание маленькой обложки
        logger.info("Was invoked method for generate user avatar preview");
        try (InputStream inputStream = Files.newInputStream(filePath);
             BufferedInputStream bis = new BufferedInputStream(inputStream, 1024);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BufferedImage image = ImageIO.read(bis);

            int height = image.getHeight() / (image.getWidth() / 100);
            BufferedImage preview = new BufferedImage(100, height, image.getType());
            Graphics2D graphics = preview.createGraphics();
            graphics.drawImage(image, 0, 0, 100, height, null);
            graphics.dispose();
            ImageIO.write(preview, getExtension(filePath.getFileName().toString()), baos);
            return baos.toByteArray();
        }
    }

    private String getExtension(String fileName) {
        logger.info("Was invoked method for get extension");
        logger.debug("Was invoked method getExtension for filename: {}", fileName);
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}
