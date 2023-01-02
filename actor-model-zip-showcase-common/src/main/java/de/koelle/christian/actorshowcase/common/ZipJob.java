package de.koelle.christian.actorshowcase.common;

import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        Map<String, ZipInputStreamSupplier> filesToBeIncluded,
        Set<ZipJob> subZipJobs) {


    public String toString() {
        return toStringSimple(0);
    }


    public String toStringHierarchical() {
        return "ZipJob:\n%s%n%s\n%s"
                .formatted(
                        StringUtils.leftPad("", 200, "-"),
                        toStringHierarchical(0),
                        StringUtils.leftPad("", 200, "-")
                );
    }

    private String toStringSimple(int depth) {
        final List<String> subZipJobsToString = subZipJobs
                .stream()
                .map(i -> i.toStringSimple(depth + 1))
                .toList();
        return List.of(
                        "targetFolderPath=%s".formatted(targetFolderPath),
                        "targetZipFileName=%s".formatted(targetZipFileName),
                        "amountFilesToBeIncluded=%s".formatted(filesToBeIncluded.size()),
                        "subZipJobs=%s".formatted(subZipJobsToString)
                )
                .stream()
                .collect(Collectors.joining(", "));
    }

    private String toStringHierarchical(int depth) {
        final List<String> subZipJobsToString = subZipJobs
                .stream()
                .map(i -> "\n" + i.toStringHierarchical(depth + 1))
                .toList();
        return List.of(
                        "targetFolderPath=%s".formatted(targetFolderPath),
                        "targetZipFileName=%s".formatted(targetZipFileName),
                        "filesToBeIncluded=%s".formatted(filesToBeIncluded.keySet()),
                        "subZipJobs=%s".formatted(subZipJobsToString)
                )
                .stream()
                .map(i -> StringUtils.leftPad("", depth * 4, " ") + i)
                .collect(Collectors.joining("\n"));
    }

}
