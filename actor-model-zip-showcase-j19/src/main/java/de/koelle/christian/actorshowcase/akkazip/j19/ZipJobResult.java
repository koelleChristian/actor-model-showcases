package de.koelle.christian.actorshowcase.akkazip.j19;

import de.koelle.christian.actorshowcase.common.ZipJob;

import java.nio.file.Path;

public record ZipJobResult(ZipJob zipJob, Path tempResultPath) {
}