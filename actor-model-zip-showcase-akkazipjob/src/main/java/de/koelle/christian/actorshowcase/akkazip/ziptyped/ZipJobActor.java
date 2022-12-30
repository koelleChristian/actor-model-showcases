package de.koelle.christian.actorshowcase.akkazip.ziptyped;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import de.koelle.christian.actorshowcase.akkazip.nonakka.Preconditions;
import de.koelle.christian.actorshowcase.akkazip.nonakka.ZipJob;
import de.koelle.christian.actorshowcase.akkazip.ziptyped.ZipExecutionActor.ZipExcecutionActorRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ZipJobActor extends AbstractBehavior<ZipJobActor.Command> {

    public interface Command {
    }

    public record ZipJobActorRequest(ZipJob zipJob) implements ZipJobActor.Command, ZipMainActor.Command {
    }

    public record ZipJobActorResponse(ZipJob zipJob,
                                      ZipExecutionActor.ZipExecutionActorResponse zipExecutionActorResponse)
            implements ZipJobActor.Command, ZipMainActor.Command {
    }

    private final ActorRef<Command> requester;
    private ZipJob mainJobReceived;
    private Map<ZipJob, ZipJobActorResponse> subZipJobRepliesSoFar = new LinkedHashMap<>();
    private Set<ZipJob> pendingSubZipJobReplies = new LinkedHashSet<>();

    public static Behavior<ZipJobActor.Command> create(ActorRef<Command> requester) {
        return Behaviors.setup(context -> new ZipJobActor(context, requester));
    }

    private ZipJobActor(ActorContext<Command> context, ActorRef<Command> requester) {
        super(context);
        this.requester = requester;
        context.getLog().info("ZipJobActor actor started");
    }

    @Override
    public Receive<ZipJobActor.Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ZipJobActor.ZipJobActorRequest.class, this::onZipActorRequest)
                .onMessage(ZipJobActor.ZipJobActorResponse.class, this::onSubZipActorResponse)
                .onMessage(ZipExecutionActor.ZipExecutionActorResponse.class, this::onZipExecutionResponse)
                .build();
    }

    private Behavior<ZipJobActor.Command> onZipActorRequest(ZipJobActor.ZipJobActorRequest request) {
        getContext().getLog().info("onZipActorRequest(): {}", request);

        final ZipJob zipJob = request.zipJob();
        this.mainJobReceived = zipJob;
        final Set<ZipJob> subZipJobs = zipJob.subZipJobs();

        // spawn sub jobs
        for (ZipJob subZipJob : subZipJobs) {
            final ActorRef<Command> commandActorRef = getContext().spawnAnonymous(ZipJobActor.create(getContext().getSelf()));
            commandActorRef.tell(new ZipJobActorRequest(subZipJob));
        }
        pendingSubZipJobReplies.addAll(subZipJobs);

        zipWhenAllSubZipsCollectedIfAny();
        return this;
    }

    private Behavior<ZipJobActor.Command> onSubZipActorResponse(ZipJobActor.ZipJobActorResponse response) {
        getContext().getLog().info("onSubZipActorResponse(): {}", response);

        final ZipJob subZipJobFinished = response.zipJob();
        subZipJobRepliesSoFar.put(subZipJobFinished, response);
        pendingSubZipJobReplies.remove(subZipJobFinished);

        zipWhenAllSubZipsCollectedIfAny();
        return this;
    }

    private Behavior<ZipJobActor.Command> onZipExecutionResponse(ZipExecutionActor.ZipExecutionActorResponse response) {
        getContext().getLog().info("onZipExecutionResponse(): {}", response);
        Preconditions.checkState(pendingSubZipJobReplies.isEmpty());

        mainJobReceived.targetFolderPath()
                .ifPresent(i -> {
                    try {
                        final Path targetFilePath = i.resolve(mainJobReceived.targetZipFileName() + ".zip");
                        Files.deleteIfExists(targetFilePath);
                        Files.copy(response.tempResultPath(), targetFilePath);
                        getContext().getLog().info("File written(): {}", targetFilePath);
                    } catch (IOException e) {
                        throw new IllegalStateException("Unhandled exception occurred.", e);
                    }
                });

        if (mainJobReceived.targetFolderPath().isPresent()) {

        }
        requester.tell(new ZipJobActor.ZipJobActorResponse(mainJobReceived, response));
        return Behaviors.stopped();
    }

    private Behavior<ZipJobActor.Command> zipWhenAllSubZipsCollectedIfAny() {
        if (pendingSubZipJobReplies.isEmpty()) {
            final ActorRef<ZipExecutionActor.Command> commandActorRef = getContext().spawnAnonymous(ZipExecutionActor.create());
            final String jobName = mainJobReceived.targetZipFileName();
            Map<String, Path> filesToBeZippedFinally = assembleAllFilesPathsToBeZipped(mainJobReceived, subZipJobRepliesSoFar);
            commandActorRef.tell(new ZipExcecutionActorRequest(jobName, filesToBeZippedFinally, getContext().getSelf().unsafeUpcast()));
        }
        return this;
    }

    private Map<String, Path> assembleAllFilesPathsToBeZipped(final ZipJob mainJobReceivedLocal, final Map<ZipJob, ZipJobActorResponse> subZipJobRepliesSoFarLocal) {
        final Map<String, Path> filePathsToBeZippedDirectly = mainJobReceivedLocal.filesToBeIncluded().stream()
                .collect(Collectors.toMap(
                        i -> i.getFileName().toString(),
                        i -> i
                ));
        final Map<String, Path> filePathsToBeZippedFromSubJobs = subZipJobRepliesSoFarLocal.values()
                .stream()
                .collect(Collectors.toMap(
                        i -> i.zipExecutionActorResponse().job().jobName() + ".zip",
                        i -> i.zipExecutionActorResponse().tempResultPath()
                ));
        Map<String, Path> filesToBeZippedFinally = new HashMap<>();
        filesToBeZippedFinally.putAll(filePathsToBeZippedFromSubJobs);
        filesToBeZippedFinally.putAll(filePathsToBeZippedDirectly);
        return filesToBeZippedFinally;
    }

}
