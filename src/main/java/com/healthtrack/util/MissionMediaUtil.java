package com.healthtrack.util;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class MissionMediaUtil {
    private static final DateTimeFormatter CARD_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM", Locale.FRENCH);
    private static final Path IMAGE_UPLOAD_DIR = Paths.get("uploaded-images");
    private static final List<Path> LEGACY_IMAGE_SEARCH_ROOTS = List.of(
            Paths.get(""),
            Paths.get("target"),
            Paths.get(System.getProperty("user.home"), "Pictures"),
            Paths.get(System.getProperty("user.home"), "Downloads"),
            Paths.get(System.getProperty("user.home"), "Desktop"),
            Paths.get(System.getProperty("java.io.tmpdir"))
    );
    private static final Map<String, Path> IMAGE_PATH_CACHE = new HashMap<>();

    private MissionMediaUtil() {
    }

    public static String importMissionPhoto(File selectedFile) {
        try {
            Files.createDirectories(IMAGE_UPLOAD_DIR);
            String fileName = System.currentTimeMillis() + "_" + selectedFile.getName().replaceAll("\\s+", "_");
            Path target = IMAGE_UPLOAD_DIR.resolve(fileName);
            Files.copy(selectedFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString().replace("\\", "/");
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'importer l'image: " + selectedFile.getAbsolutePath(), e);
        }
    }

    public static ImageView createMissionImageView(String storedPath, double targetWidth, double targetHeight) {
        Path imagePath = resolveMissionImagePath(storedPath);
        if (imagePath == null || !Files.exists(imagePath)) {
            return null;
        }

        Image image = new Image(imagePath.toUri().toString());
        if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return null;
        }

        ImageView imageView = new ImageView(image);
        imageView.setViewport(computeCoverViewport(image.getWidth(), image.getHeight(), targetWidth, targetHeight));
        imageView.setFitWidth(targetWidth);
        imageView.setFitHeight(targetHeight);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.setCache(true);
        return imageView;
    }

    public static String formatMissionDate(LocalDate startDate, LocalDate endDate) {
        if (startDate != null) {
            return CARD_DATE_FORMAT.format(startDate);
        }
        if (endDate != null) {
            return CARD_DATE_FORMAT.format(endDate);
        }
        return "Date a definir";
    }

    public static String buildMissionPeriodText(LocalDate startDate, LocalDate endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (startDate != null && endDate != null) {
            return "Du " + startDate.format(formatter) + " au " + endDate.format(formatter);
        }
        if (startDate != null) {
            return "A partir du " + startDate.format(formatter);
        }
        if (endDate != null) {
            return "Jusqu'au " + endDate.format(formatter);
        }
        return "Dates a definir";
    }

    public static String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static int buildMatchPercent(int missionId, String title) {
        int seed = Math.abs((missionId * 17) + safeText(title, "").length() * 11);
        return 35 + (seed % 25);
    }

    private static Rectangle2D computeCoverViewport(double imageWidth, double imageHeight,
                                                    double targetWidth, double targetHeight) {
        double imageRatio = imageWidth / imageHeight;
        double targetRatio = targetWidth / targetHeight;

        if (imageRatio > targetRatio) {
            double viewportWidth = imageHeight * targetRatio;
            double x = (imageWidth - viewportWidth) / 2.0;
            return new Rectangle2D(x, 0, viewportWidth, imageHeight);
        }

        double viewportHeight = imageWidth / targetRatio;
        double y = (imageHeight - viewportHeight) / 2.0;
        return new Rectangle2D(0, y, imageWidth, viewportHeight);
    }

    private static Path resolveMissionImagePath(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }

        String normalized = storedPath.trim().replace("\\", "/");
        if (IMAGE_PATH_CACHE.containsKey(normalized)) {
            return IMAGE_PATH_CACHE.get(normalized);
        }

        Path directPath = Paths.get(normalized);
        if (!directPath.isAbsolute()) {
            directPath = Paths.get("").resolve(normalized).normalize();
        }
        if (Files.exists(directPath)) {
            IMAGE_PATH_CACHE.put(normalized, directPath);
            return directPath;
        }

        Path fileNamePath = Paths.get(normalized).getFileName();
        String fileName = fileNamePath == null ? normalized : fileNamePath.toString();
        Path uploadedImage = IMAGE_UPLOAD_DIR.resolve(fileName).normalize();
        if (Files.exists(uploadedImage)) {
            IMAGE_PATH_CACHE.put(normalized, uploadedImage);
            return uploadedImage;
        }

        Path fallback = findLegacyImageByName(fileName);
        IMAGE_PATH_CACHE.put(normalized, fallback);
        return fallback;
    }

    private static Path findLegacyImageByName(String fileName) {
        for (Path root : LEGACY_IMAGE_SEARCH_ROOTS) {
            if (!Files.exists(root)) {
                continue;
            }
            try (Stream<Path> pathStream = Files.walk(root, 4)) {
                Path match = pathStream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName() != null)
                        .filter(path -> path.getFileName().toString().equalsIgnoreCase(fileName))
                        .findFirst()
                        .orElse(null);
                if (match != null) {
                    return match;
                }
            } catch (IOException | UncheckedIOException ignored) {
                // Continue with the next root.
            }
        }
        return null;
    }
}
