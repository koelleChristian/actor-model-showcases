package de.koelle.christian.actorshowcase.akkazip.ziptyped.commands;

import akka.actor.typed.ActorRef;
import de.koelle.christian.actorshowcase.common.ZipInputStreamSupplier;

import java.util.Map;

public record ZipExcecutionActorRequest(String jobName,
                                        Map<String, ZipInputStreamSupplier> zipInputStreamSupplierByTargetFileName,
                                        ActorRef<ZipExecutionActorResponse> replyTo)
        implements Command {
}