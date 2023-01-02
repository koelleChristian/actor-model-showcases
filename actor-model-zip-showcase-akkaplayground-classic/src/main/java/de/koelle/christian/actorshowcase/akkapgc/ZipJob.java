package de.koelle.christian.actorshowcase.akkapgc;

import java.nio.file.Path;
import java.util.Set;

public record ZipJob( String jobName, Set<Path> filePathsToBeZipped) {
}
