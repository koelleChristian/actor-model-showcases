package de.koelle.christian.actorshowcase.akkazip.nonakka;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class TestdataSupplier {

    private final OurFileUtils ourFileUtils = new OurFileUtils();

    public ZipJob getHierarchicalZipJob(Path targetFolderPath, String targetZipName) {
        final Map<String, Map<String, Set<Path>>> partitionedExampleFilePaths = getPartitionedExampleFilePaths();

        final Set<ZipJob> nestedZipJobs = partitionedExampleFilePaths.entrySet()
                .stream()
                .map(i -> {
                            final Set<ZipJob> nestedNestedZipJobs = i.getValue().entrySet()
                                    .stream()
                                    .map(j -> new ZipJob(
                                            Optional.empty(),
                                            "%s_%s".formatted(i.getKey(), j.getKey()),
                                            j.getValue(),
                                            Set.of())
                                    )
                                    .collect(Collectors.toSet());
                            return new ZipJob(
                                    Optional.empty(),
                                    i.getKey(),
                                    Set.of(),
                                    nestedNestedZipJobs);
                        }
                )
                .collect(Collectors.toSet());
        return new ZipJob(
                Optional.of(targetFolderPath),
                targetZipName,
                Set.of(),
                nestedZipJobs);
    }

    public Map<String, Map<String, Set<Path>>> getPartitionedExampleFilePaths() {
        final Set<Path> allFilePaths = getExampleFilePaths();
        final Map<String, Map<String, Set<Path>>> allFileNamesByKeys = new HashMap<>();
        for (Path filePath : allFilePaths) {
            final String[] s = filePath.getFileName().toString().split("_");
            final String filePrefix1 = s[0];
            final String filePrefix2 = s[1];
            allFileNamesByKeys.putIfAbsent(filePrefix1, new HashMap<>());
            allFileNamesByKeys.get(filePrefix1).putIfAbsent(filePrefix2, new TreeSet<>());
            allFileNamesByKeys.get(filePrefix1).get(filePrefix2).add(filePath);
        }
        return allFileNamesByKeys;
    }

    public Set<Path> getExampleFilePaths() {
        try {
            Path pictureFolderPathSource = Path.of("src/main/resources/images");
            Path pictureFolderPathWorkdir = Path.of("target/images");
            FileUtils.deleteDirectory(new File(pictureFolderPathWorkdir.toUri()));

            Set<Path> originalFilePaths = ourFileUtils.getFilesFromPath(pictureFolderPathSource, i -> true, FileClass.PICTURES);
            for (Path originalFilePath : originalFilePaths) {
                Path originalFileSubPath = originalFilePath.subpath(4, originalFilePath.getNameCount());
                Path targetFilePath = pictureFolderPathWorkdir.resolve(originalFileSubPath);
                Files.createDirectories(targetFilePath.getParent());
                Files.copy(originalFilePath, targetFilePath);
            }
            return ourFileUtils.getFilesFromPath(pictureFolderPathWorkdir, FileClass.PICTURES);
        } catch (IOException e) {
            throw new IllegalStateException("Unhandled exception occurred.", e);
        }
    }
}
