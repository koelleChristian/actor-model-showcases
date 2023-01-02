package de.koelle.christian.actorshowcase.akkapgc;

import java.nio.file.Path;

public record ZipJobResult(ZipJob job, Path tempResultPath) {
}
