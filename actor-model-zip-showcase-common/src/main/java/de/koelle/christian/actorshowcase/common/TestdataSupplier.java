package de.koelle.christian.actorshowcase.common;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestdataSupplier {

    private static final String SAMPLE_FILES_PATH = "../sharedresources/samplefiles";
    private static final Path ONE_FIXED_SAMPLE_FILE_PATH = Paths.get(SAMPLE_FILES_PATH)
            .resolve("elsewhat/Notes1.txt");
    private final OurFileUtils ourFileUtils = new OurFileUtils();

    public ZipJob createHierarchicalZipJob(Path targetFolderPath, String targetZipName, int depth, int width, final boolean persistSubZips) {
       return createHierarchicalZipJobSubRoutine(targetFolderPath, targetZipName, depth, width, persistSubZips, "");
    }
    private ZipJob createHierarchicalZipJobSubRoutine(Path targetFolderPath, String targetZipName, int depth, int width, final boolean persistSubZips, final String previousZipFileIndexNameChunk) {
        final int widthAmountOfAmountCharacters = String.valueOf(width).length();

        final BiFunction<String, String, String> dynFileNameFktn = (number, fileExtenstion) -> String.format(
                "File_%s%s",
                StringUtils.leftPad(number, widthAmountOfAmountCharacters, "0"), fileExtenstion
        );

        LinkedHashMap<String, ZipInputStreamSupplier> filesToBeIncluded = IntStream
                .range(1, width + 1)
                .mapToObj(String::valueOf)
                .map(i ->
                        Pair.of(
                                dynFileNameFktn.apply(i, ".txt"),
                                TechnicalZipperSupport.getIssForFilePath(ONE_FIXED_SAMPLE_FILE_PATH)
                        )
                )
                .collect(Collectors.toMap(
                        Pair::getLeft,
                        Pair::getRight,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        Set<ZipJob> subZipJobs = Set.of();
        if (depth >= 1) {
            Path targetFolderPathForSubJobs = persistSubZips ?
                    targetFolderPath :
                    null;

            subZipJobs = IntStream
                    .range(1, width + 1)
                    .mapToObj(String::valueOf)
                    .map(i -> {
                        final int nextDepth = depth - 1;
                        final String thisZipFileNameIndexChunk = StringUtils.isBlank(previousZipFileIndexNameChunk) ?
                                i:
                                String.join("_", previousZipFileIndexNameChunk, i);
                        final String subZipName = dynFileNameFktn.apply(thisZipFileNameIndexChunk, "");
                        return createHierarchicalZipJobSubRoutine(targetFolderPathForSubJobs, subZipName, nextDepth, width, persistSubZips, thisZipFileNameIndexChunk);
                    })
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        return new ZipJob(
                Optional.ofNullable(targetFolderPath),
                targetZipName,
                filesToBeIncluded,
                subZipJobs
        );
    }


    public Set<Path> getExampleFilePaths(FileClass fileClass) {
        return ourFileUtils.getFilesFromPath(Path.of(SAMPLE_FILES_PATH), fileClass);
    }
}
