package de.koelle.christian.actorshowcase.akkazip.j19;

import de.koelle.christian.actorshowcase.common.ConditionalRuntimeExceptionProvoker;
import de.koelle.christian.actorshowcase.common.TechnicalZipper;
import de.koelle.christian.actorshowcase.common.TechnicalZipperSupport;
import de.koelle.christian.actorshowcase.common.ZipInputStreamSupplier;
import de.koelle.christian.actorshowcase.common.ZipJob;
import jdk.incubator.concurrent.StructuredTaskScope;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class ZipJobExecutor {

    private static final Logger LOG = LogManager.getLogger(ZipJobExecutor.class);
    private final TechnicalZipper technicalZipper = new TechnicalZipper();
    private final ConditionalRuntimeExceptionProvoker conditionalRuntimeExceptionProvoker = new ConditionalRuntimeExceptionProvoker();
    private final LoggingUtils loggingUtils = new LoggingUtils();

    public ZipJobResult doJob(ZipJob zipJob) {
        try {
            return process(zipJob);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Top level exception caught:", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Top level exception caught:", e);
        }
    }

    public ZipJobResult process(ZipJob thisZipJob) throws ExecutionException, InterruptedException {
        loggingUtils.log(LOG, Level.INFO, "process()");
        final List<ZipJobResult> subZipResponses = processSub(thisZipJob.subZipJobs());
        final Map<String, ZipInputStreamSupplier> filePathsToBeZippedByTargetName = assembleAllFilesPathsToBeZipped(thisZipJob, subZipResponses);
        final Path thisZipJobTempResultZipFilePath = technicalZipper.createZipAsTemporaryFile(filePathsToBeZippedByTargetName);
        saveZipFileToTargetIfSpecified(thisZipJob, thisZipJobTempResultZipFilePath);
        deleteInterimTempFiles(subZipResponses);
        return new ZipJobResult(thisZipJob, thisZipJobTempResultZipFilePath);
    }


    public List<ZipJobResult> processSub(Set<ZipJob> subZipJobs) throws ExecutionException, InterruptedException {

        if (subZipJobs.isEmpty()) {
            return List.of();
        }

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            final List<Future<ZipJobResult>> futures = subZipJobs
                    .stream()
                    .map(i -> scope.fork(
                            () -> process((i))
                    ))
                    .toList();

            // Wait for results or errors
            loggingUtils.log(LOG, Level.INFO, "sub-zip-jobs: waiting to finish");
            scope.join();
            scope.throwIfFailed();

            // Collect results on success
            final List<ZipJobResult> subResults = futures
                    .stream()
                    .map(Future::resultNow)
                    .toList();
            loggingUtils.log(LOG, Level.INFO, "sub-zip-jobs: Done (amount=%s)".formatted(subResults.size()));

            conditionalRuntimeExceptionProvoker.throwConditionalExceptionWhenEnabledForTesting(fn -> {
                final List<String> resultZipNames = subResults
                        .stream()
                        .map(s -> s.zipJob().targetZipFileName())
                        .toList();
                return resultZipNames.contains(fn);
            });

            return subResults;
        }
    }

    private Map<String, ZipInputStreamSupplier> assembleAllFilesPathsToBeZipped(final ZipJob mainJobReceivedLocal,
                                                                                final List<ZipJobResult> subZipJobRepliesSoFarLocal) {
        final Map<String, ZipInputStreamSupplier> filePathsToBeZippedDirectly = mainJobReceivedLocal.filesToBeIncluded();
        final Map<String, ZipInputStreamSupplier> filePathsToBeZippedFromSubJobs = subZipJobRepliesSoFarLocal
                .stream()
                .collect(Collectors.toMap(
                        i -> i.zipJob().targetZipFileName() + ".zip",
                        i -> TechnicalZipperSupport.getIssForFilePath(i.tempResultPath())
                ));
        Map<String, ZipInputStreamSupplier> filesToBeZippedFinally = new HashMap<>();
        filesToBeZippedFinally.putAll(filePathsToBeZippedFromSubJobs);
        filesToBeZippedFinally.putAll(filePathsToBeZippedDirectly);
        return filesToBeZippedFinally;
    }


    private void saveZipFileToTargetIfSpecified(ZipJob thisZipJob, Path thisZipJobTempResultZipFilePath) {
        thisZipJob.targetFolderPath()
                .ifPresent(i -> {
                    try {
                        final Path targetFilePath = i.resolve(thisZipJob.targetZipFileName() + ".zip");
                        Files.deleteIfExists(targetFilePath);
                        Files.createDirectories(i);
                        Files.copy(thisZipJobTempResultZipFilePath, targetFilePath);
                        loggingUtils.log(LOG, Level.INFO, "File written(): %s.".formatted(targetFilePath));
                    } catch (IOException e) {
                        throw new IllegalStateException("Error writing file.", e);
                    }
                });
    }

    private void deleteInterimTempFiles(List<ZipJobResult> subZipResponses) {
        final List<Path> temporaryFilePathsFromSubJobs = subZipResponses
                .stream()
                .map((i -> i.tempResultPath()))
                .toList();
        for (Path temporaryFilePathsFromSubJob : temporaryFilePathsFromSubJobs) {
            try {
                Files.deleteIfExists(temporaryFilePathsFromSubJob);
                loggingUtils.log(LOG, Level.INFO, "Temporary sub zip deleted: %s.".formatted(temporaryFilePathsFromSubJob));
            } catch (IOException e) {
                throw new IllegalStateException("Unhandled exception occurred.", e);
            }
        }
    }
}
