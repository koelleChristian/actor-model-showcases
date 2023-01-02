package de.koelle.christian.actorshowcase.akkazip.ziptyped;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.ChildFailed;
import akka.actor.typed.javadsl.*;
import de.koelle.christian.actorshowcase.akkazip.ziptyped.commands.*;
import de.koelle.christian.actorshowcase.common.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class ZipJobActor extends AbstractBehavior<Command> {

    private final ActorRef<Command> requester;
    private final Duration timeout;
    private ZipJob mainJobReceived;
    private final Map<ZipJob, ZipJobActorResponse> subZipJobRepliesSoFar = new LinkedHashMap<>();
    private final Set<ZipJob> pendingSubZipJobReplies = new LinkedHashSet<>();
    private final ConditionalRuntimeExceptionProvoker conditionalRuntimeExceptionProvoker = new ConditionalRuntimeExceptionProvoker();

    public static Behavior<Command> create(ActorRef<Command> requester, Duration timeout) {
        return Behaviors.setup(
                context -> Behaviors.withTimers(
                        timers -> new ZipJobActor(context, requester, timers, timeout)
                )
        );
    }

    private ZipJobActor(ActorContext<Command> context, ActorRef<Command> requester, TimerScheduler<Command> timers, Duration timeout) {
        super(context);
        this.requester = requester;
        this.timeout = timeout;
        timers.startSingleTimer(new Timeout(), timeout);
        context.getLog().info("ZipJobActor actor started");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ZipJobActorRequest.class, this::onZipActorRequest)
                .onMessage(ZipJobActorResponse.class, this::onSubZipActorResponse)
                .onMessage(ZipExecutionActorResponse.class, this::onZipExecutionResponse)
                .onMessage(Timeout.class, this::onTimeout)
                // Bubble failures up through the hierarchy:
                // https://doc.akka.io/docs/akka/current/typed/fault-tolerance.html#bubble-failures-up-through-the-hierarchy
                .onSignal(ChildFailed.class, this::onChildFailed) // in conjunction with getContext().watch(commandActorRef);
                .build();
    }


    private Behavior<Command> onZipActorRequest(ZipJobActorRequest request) {
        getContext().getLog().info("onZipActorRequest(): {}", request);

        final ZipJob zipJob = request.zipJob();
        this.mainJobReceived = zipJob;
        final Set<ZipJob> subZipJobs = zipJob.subZipJobs();

        // spawn sub jobs
        for (ZipJob subZipJob : subZipJobs) {
            // spawnAnonymous: see https://doc.akka.io/docs/akka/current/typed/from-classic.html#actorof-and-props
            final ActorRef<Command> commandActorRef = getContext().spawnAnonymous(ZipJobActor.create(getContext().getSelf(), timeout));
            getContext().watch(commandActorRef); // in conjunction with .onSignal(ChildFailed.class, this::onChildFailed)
            commandActorRef.tell(new ZipJobActorRequest(subZipJob));
        }
        pendingSubZipJobReplies.addAll(subZipJobs);

        zipWhenAllSubZipsCollectedIfAny();
        return this;
    }

    private Behavior<Command> onSubZipActorResponse(ZipJobActorResponse response) {
        getContext().getLog().info("onSubZipActorResponse(): {}", response);

        final ZipJob subZipJobFinished = response.zipJob();
        subZipJobRepliesSoFar.put(subZipJobFinished, response);
        pendingSubZipJobReplies.remove(subZipJobFinished);

        zipWhenAllSubZipsCollectedIfAny();
        return this;
    }

    private Behavior<Command> onZipExecutionResponse(ZipExecutionActorResponse response) {
        getContext().getLog().info("onZipExecutionResponse(): {}", response);
        Preconditions.checkState(pendingSubZipJobReplies.isEmpty());

        conditionalRuntimeExceptionProvoker.throwConditionalExceptionWhenEnabledForTesting(fn -> fn.equals(response.job().jobName()));

        mainJobReceived.targetFolderPath()
                .ifPresent(i -> {
                    try {
                        final Path targetFilePath = i.resolve(mainJobReceived.targetZipFileName() + ".zip");
                        Files.deleteIfExists(targetFilePath);
                        Files.createDirectories(i);
                        Files.copy(response.tempResultPath(), targetFilePath);
                        getContext().getLog().info("File written(): {}", targetFilePath);
                    } catch (IOException e) {
                        throw new IllegalStateException("Unhandled exception occurred.", e);
                    }
                });

        final List<Path> temporaryFilePathsFromSubJobs = subZipJobRepliesSoFar.values()
                .stream()
                .map((i -> i.zipExecutionActorResponse().tempResultPath()))
                .toList();
        for (Path temporaryFilePathsFromSubJob : temporaryFilePathsFromSubJobs) {
            try {
                Files.deleteIfExists(temporaryFilePathsFromSubJob);
                getContext().getLog().info("Temporary sub zip deleted: {}", temporaryFilePathsFromSubJob);
            } catch (IOException e) {
                throw new IllegalStateException("Unhandled exception occurred.", e);
            }
        }
        requester.tell(new ZipJobActorResponse(mainJobReceived, response));
        return Behaviors.stopped();
    }

    private Behavior<Command> onTimeout(Timeout timeout) {
        throw new IllegalStateException("Timeout occourred.");
    }

    private Behavior<Command> onChildFailed(ChildFailed childFailed) {
        childFailed.getCause().printStackTrace();
        getContext().getLog().info("child failed: {}", childFailed.getCause());
        if (childFailed.cause() instanceof RuntimeException rte) {
            throw rte;
        } else {
            throw new IllegalStateException(childFailed.cause());
        }
    }

    private Behavior<Command> zipWhenAllSubZipsCollectedIfAny() {
        if (pendingSubZipJobReplies.isEmpty()) {
            final ActorRef<Command> commandActorRef = getContext().spawnAnonymous(ZipExecutionActor.create());
            final String jobName = mainJobReceived.targetZipFileName();
            // Without 'getContext().watch(commandActorRef)' the system will not terminate on a child error
            getContext().watch(commandActorRef);
            Map<String, ZipInputStreamSupplier> filesToBeZippedFinally = assembleAllFilesPathsToBeZipped(mainJobReceived, subZipJobRepliesSoFar);
            commandActorRef.tell(new ZipExcecutionActorRequest(jobName, filesToBeZippedFinally, getContext().getSelf().unsafeUpcast()));
        }
        return this;
    }

    private Map<String, ZipInputStreamSupplier> assembleAllFilesPathsToBeZipped(final ZipJob mainJobReceivedLocal, final Map<ZipJob, ZipJobActorResponse> subZipJobRepliesSoFarLocal) {
        final Map<String, ZipInputStreamSupplier> filePathsToBeZippedDirectly = mainJobReceivedLocal.filesToBeIncluded();
        final Map<String, ZipInputStreamSupplier> filePathsToBeZippedFromSubJobs = subZipJobRepliesSoFarLocal.values()
                .stream()
                .collect(Collectors.toMap(
                        i -> i.zipExecutionActorResponse().job().jobName() + ".zip",
                        i -> TechnicalZipperSupport.getIssForFilePath(i.zipExecutionActorResponse().tempResultPath())
                ));
        Map<String, ZipInputStreamSupplier> filesToBeZippedFinally = new HashMap<>();
        filesToBeZippedFinally.putAll(filePathsToBeZippedDirectly);
        filesToBeZippedFinally.putAll(filePathsToBeZippedFromSubJobs);
        return filesToBeZippedFinally;
    }

}
