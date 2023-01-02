package de.koelle.christian.actorshowcase.akkazip.ziptyped.commands;

import de.koelle.christian.actorshowcase.common.ZipJob;

public record ZipJobActorResponse(ZipJob zipJob,
                                  ZipExecutionActorResponse zipExecutionActorResponse)
        implements Command {
}