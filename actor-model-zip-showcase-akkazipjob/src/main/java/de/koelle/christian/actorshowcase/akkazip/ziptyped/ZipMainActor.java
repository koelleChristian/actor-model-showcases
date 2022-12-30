package de.koelle.christian.actorshowcase.akkazip.ziptyped;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import de.koelle.christian.actorshowcase.akkazip.nonakka.TestdataSupplier;
import de.koelle.christian.actorshowcase.akkazip.nonakka.ZipJob;
import de.koelle.christian.actorshowcase.akkazip.ziptyped.ZipJobActor.ZipJobActorResponse;

import java.nio.file.Paths;

public class ZipMainActor extends AbstractBehavior<ZipMainActor.Command> {

    private final TestdataSupplier testdataSupplier = new TestdataSupplier();

    public static void main(String[] args) {
        ActorRef<Command> testSystem = ActorSystem.create(ZipMainActor.create(), "zip-job-execution");
        testSystem.tell(MainCommands.START);
    }

    public interface Command {
    }

    public enum MainCommands implements Command {
        START,
        /**/;
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(ZipMainActor::new);
    }

    private ZipMainActor(ActorContext<Command> context) {
        super(context);
        context.getLog().info("ZipMainActor actor [BGN]");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals(MainCommands.START, this::onStart)
                .onMessageUnchecked(ZipJobActorResponse.class, this::onResult)
                .build();
    }

    private Behavior<Command> onStart() {
        final ActorRef<ZipJobActor.Command> commandActorRef = getContext().spawnAnonymous(ZipJobActor.create(getContext().getSelf().unsafeUpcast()));
        final ZipJob job = testdataSupplier.getHierarchicalZipJob(Paths.get("target/"), "result");
        commandActorRef.tell(new ZipJobActor.ZipJobActorRequest(job));
        return this;
    }

    private Behavior<Command> onResult(ZipJobActorResponse response) {
        final ZipExecutionActor.ZipExecutionActorResponse zipExecutionActorResponse = response.zipExecutionActorResponse();
        getContext().getLog().info("-------------------------");
        getContext().getLog().info("Result: name={} path={}", zipExecutionActorResponse.job().jobName(), zipExecutionActorResponse.tempResultPath());
        getContext().getLog().info("-------------------------");
        getContext().getLog().info("ZipMainActor actor [END]");
        return Behaviors.stopped();
    }



}
