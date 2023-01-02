package de.koelle.christian.actorshowcase.akkazip.ziptyped;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import de.koelle.christian.actorshowcase.akkazip.ziptyped.commands.Command;
import de.koelle.christian.actorshowcase.akkazip.ziptyped.commands.ZipExcecutionActorRequest;
import de.koelle.christian.actorshowcase.akkazip.ziptyped.commands.ZipExecutionActorResponse;
import de.koelle.christian.actorshowcase.common.TechnicalZipper;
import de.koelle.christian.actorshowcase.common.ZipInputStreamSupplier;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.util.Map;

public class ZipExecutionActor extends AbstractBehavior<Command> {
    private final TechnicalZipper technicalZipper = new TechnicalZipper();

    private String jobName;

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new ZipExecutionActor(context));
    }

    private ZipExecutionActor(ActorContext<Command> context) {
        super(context);
        context.getLog().info("ZipExecutionActor actor started");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ZipExcecutionActorRequest.class, this::onZipExecutionActorRequest)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onZipExecutionActorRequest(ZipExcecutionActorRequest request) {
        getContext().getLog().info("onZipExecutionActorRequest(): {}", request);
        this.jobName = request.jobName();
        final Map<String, ZipInputStreamSupplier> thingsToBeZipped = request.zipInputStreamSupplierByTargetFileName();
        final Path zipAsTemporaryFile = technicalZipper.createZipAsTemporaryFile(thingsToBeZipped);
        request.replyTo().tell(new ZipExecutionActorResponse(request, zipAsTemporaryFile));
        return Behaviors.stopped(); // All work is done
    }

    private ZipExecutionActor onPostStop() {
        getContext().getLog().info("{} actor stopped: jobName={}", FilenameUtils.getBaseName(getClass().getSimpleName()), jobName);
        return this;
    }
}
