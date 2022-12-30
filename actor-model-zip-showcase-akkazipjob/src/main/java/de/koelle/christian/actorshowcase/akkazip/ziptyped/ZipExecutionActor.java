package de.koelle.christian.actorshowcase.akkazip.ziptyped;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import de.koelle.christian.actorshowcase.akkazip.nonakka.Preconditions;
import de.koelle.christian.actorshowcase.akkazip.nonakka.TechnicalZipper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

public class ZipExecutionActor extends AbstractBehavior<ZipExecutionActor.Command> {
    private final TechnicalZipper technicalZipper = new TechnicalZipper();

    public interface Command {
    }

    public record ZipExcecutionActorRequest(String jobName,
                                            // File name required, as some files are temporary files with temporary file names.
                                            Map<String, Path> filePathsToBeZippedByTargetName,
                                            ActorRef<ZipExecutionActorResponse> replyTo)
            implements ZipExecutionActor.Command {
    }

    public record ZipExecutionActorResponse(ZipExcecutionActorRequest job,
                                            Path tempResultPath) implements ZipJobActor.Command {
    }

    public static Behavior<ZipExecutionActor.Command> create() {
        return Behaviors.setup(context -> new ZipExecutionActor(context));
    }

    private ZipExecutionActor(ActorContext<ZipExecutionActor.Command> context) {
        super(context);
        context.getLog().info("ZipExecutionActor actor started");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ZipExcecutionActorRequest.class, this::onZipExecutionActorRequest)
                .build();
    }

    private Behavior<ZipExecutionActor.Command> onZipExecutionActorRequest(ZipExcecutionActorRequest request) {
        getContext().getLog().info("onZipExecutionActorRequest(): {}", request);
        final var things2BZipped = request
                .filePathsToBeZippedByTargetName().entrySet()
                .stream()
                .collect(Collectors.toMap(
                        i -> i.getKey(),
                        i -> getInputStreamSupplierByFilePath(i.getValue())
                ));
        final Path zipAsTemporaryFile = technicalZipper.createZipAsTemporaryFile(things2BZipped);
        request.replyTo().tell(new ZipExecutionActorResponse(request, zipAsTemporaryFile));
        return Behaviors.stopped(); // All work is done
    }

    private TechnicalZipper.ZipInputStreamSupplier getInputStreamSupplierByFilePath(final Path filePath) {
        Preconditions.checkNotNull(filePath);
        return () -> {
            if (Files.exists(filePath)) {
                try {
                    return Files.newInputStream(filePath);
                } catch (final IOException exception) {
                    throw new IllegalStateException("Error on creating input stream for existing file.", exception);
                }
            }
            throw new IllegalStateException("Error as file to be zipped is not available: Please check existence prior calling: %s".formatted(filePath));
        };
    }
}
