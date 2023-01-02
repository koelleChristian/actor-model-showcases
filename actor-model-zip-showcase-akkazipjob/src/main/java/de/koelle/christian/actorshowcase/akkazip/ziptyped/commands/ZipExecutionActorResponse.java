package de.koelle.christian.actorshowcase.akkazip.ziptyped.commands;

import java.nio.file.Path;

public record ZipExecutionActorResponse(ZipExcecutionActorRequest job,
                                        Path tempResultPath)
        implements Command {
}