package de.koelle.christian.actorshowcase.akkazip.ziptyped;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.ChildFailed;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import de.koelle.christian.actorshowcase.akkazip.ziptyped.commands.Command;
import de.koelle.christian.actorshowcase.akkazip.ziptyped.commands.ZipJobActorRequest;
import de.koelle.christian.actorshowcase.akkazip.ziptyped.commands.ZipJobActorResponse;
import de.koelle.christian.actorshowcase.common.TestdataSupplier;
import de.koelle.christian.actorshowcase.common.ZipJob;

import java.time.Duration;

public class ZipMainActor extends AbstractBehavior<Command> {

    private final TestdataSupplier testdataSupplier = new TestdataSupplier();
    private final Duration zipJobActorInstanceTimeout;

    public static Behavior<Command> create(Duration zipJobActorInstanceTimeout) {
        return
                /*Behaviors.setup((ActorContext<Command> context) -> new ZipMainActor(context, zipJobActorInstanceTimeout));*/
                Behaviors
                        .supervise(
                                Behaviors.setup((ActorContext<Command> context) -> new ZipMainActor(context, zipJobActorInstanceTimeout))
                        )
                        .onFailure(SupervisorStrategy.stop());
    }

    private ZipMainActor(ActorContext<Command> context, Duration zipJobActorInstanceTimeout) {
        super(context);
        this.zipJobActorInstanceTimeout = zipJobActorInstanceTimeout;
        context.getLog().info("ZipMainActor actor [BGN]");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ZipJobActorRequest.class, this::onStart)
                .onMessage(ZipJobActorResponse.class, this::onResult)
                .onSignal(ChildFailed.class, this::onChildFailed)
                .build();
    }

    private Behavior<Command> onChildFailed(ChildFailed childFailed) {
        getContext().getLog().info("child failed: {}", childFailed.toString());
       throw new IllegalStateException(childFailed.cause());
    }

    private Behavior<Command> onStart(final ZipJobActorRequest request) {
        ZipJob job = request.zipJob();

        getContext().getLog().info(job.toStringHierarchical());
        final ActorRef<Command> commandActorRef = getContext().spawnAnonymous(ZipJobActor.create(getContext().getSelf().unsafeUpcast(), zipJobActorInstanceTimeout));
        getContext().watch(commandActorRef);
        commandActorRef.tell(new ZipJobActorRequest(job));
        return this;
    }

    private Behavior<Command> onResult(ZipJobActorResponse response) {
        getContext().getLog().info("ZipMainActor actor [END]");
        return Behaviors.stopped();
    }
}
