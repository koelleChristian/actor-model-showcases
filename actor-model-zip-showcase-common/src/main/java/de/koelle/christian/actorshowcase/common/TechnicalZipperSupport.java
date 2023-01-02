package de.koelle.christian.actorshowcase.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TechnicalZipperSupport {

    public static Map<String, ZipInputStreamSupplier> getIssByFileNameForFilePaths(final Set<Path> filePaths) {
        return filePaths
                .stream()
                .map(TechnicalZipperSupport::getIssByFileNameForFilePath)
                .collect(Collectors.toMap(
                        i -> i.keySet().iterator().next(),
                        i -> i.values().iterator().next()
                ));
    }

    public static Map<String, ZipInputStreamSupplier> getIssByFileNameForFilePath(final Path filePath) {
        return Map.of(
                filePath.getFileName().toString(),
                getIssForFilePath(filePath)
        );
    }

    public static ZipInputStreamSupplier getIssForFilePath(final Path filePath) {
        Preconditions.checkNotNull(filePath);
        return () -> {
            if (Files.exists(filePath)) {
                try {
                    return Files.newInputStream(filePath);
                } catch (final IOException exception) {
                    throw new IllegalStateException("Error on creating input stream for existing file.", exception);
                }
            }
            throw new IllegalStateException("Error as file to be zipped is not available: Please check existence prior calling: %s".formatted(filePath));
        };
    }


}
