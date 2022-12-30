package de.koelle.christian.actorshowcase.akkazip.nonakka;

import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

//

/**
 * Represents a zip-file creation instruction in a hierarchical manner, i.e. you can define a job to create a zip with nested zips.
 *
 * @param targetFolderPath  An optional Path, in case the result of the job needs to be persisted.
 * @param targetZipFileName The name for the zip-File, without file-extension.
 * @param filesToBeIncluded The files to be included into the zip-File.
 * @param subZipJobs        Self reference to express, that a zip-file should be created within the target zip-File.
 */
public record ZipJob(
        Optional<Path> targetFolderPath,
        String targetZipFileName,
        Set<Path> filesToBeIncluded,
        Set<ZipJob> subZipJobs) {

    // Hierarchical display via toString()-Method
    @Override
    public String toString() {
        return toStringSub(0);
    }

    public String toStringSub(int depth) {
        final List<String> subZipJobsToString = subZipJobs
                .stream()
                .map(i -> "\n" + i.toStringSub(depth + 1))
                .toList();
        return List.of(
                        "targetFolderPath=%s".formatted(targetFolderPath),
                        "targetZipFileName=%s".formatted(targetZipFileName),
                        "filesToBeIncluded=%s".formatted(filesToBeIncluded),
                        "subZipJobs=%s".formatted(subZipJobsToString)
                )
                .stream()
                .map(i -> StringUtils.leftPad("", depth * 4, " ") + i)
                .collect(Collectors.joining("\n"));
    }

}
