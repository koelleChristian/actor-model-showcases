package de.koelle.christian.actorshowcase.akkazip;

import java.nio.file.Path;

public record ZipJobResult(ZipJob job, Path tempResultPath) {
}
