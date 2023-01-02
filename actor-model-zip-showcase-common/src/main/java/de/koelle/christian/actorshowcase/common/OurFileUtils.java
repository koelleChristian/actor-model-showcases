package de.koelle.christian.actorshowcase.common;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.TreeSet;
import java.util.function.Predicate;

public class OurFileUtils {

    public TreeSet<Path> getFilesFromPath(Path rootPathPath, FileClass fileClass) {
        return getFilesFromPath(rootPathPath, i-> !i.getFileName().toString().startsWith("._"), fileClass);
    }

    public static Path getTempFolderPathWithFallbackToCurrentFolder() {
        final String tempFolder = System.getProperty("java.io.tmpdir");
        if (StringUtils.isNotBlank(tempFolder)) {
            final Path tempFolderPath = Path.of(tempFolder);
            if (Files.exists(tempFolderPath) && Files.isDirectory(tempFolderPath)) {
                return tempFolderPath;
            }
        }
        return Path.of("");
    }

    public TreeSet<Path> getFilesFromPath(Path rootPathPath, Predicate<Path> furtherPredicates, FileClass fileClass) {
        final TreeSet<Path> allFiles = new TreeSet<>();

        Predicate<Path> furtherPredicatesNullsafe = furtherPredicates == null ? i -> true : furtherPredicates;

        String syntaxAndPattern = String.format("glob:**.{%s}", fileClass.fileExtensions);
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(syntaxAndPattern);

        try {
            Files.walkFileTree(rootPathPath, new SimpleFileVisitor<Path>() {
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                        throws IOException {
                    if (matcher.matches(path) && furtherPredicatesNullsafe.test(path)) {
                        allFiles.add(path);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return allFiles;
    }

    public void moveFile(Path source, Path target) {
        try {
            Files.createDirectories(target.getParent());
            Files.move(source, target);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

    }
}
