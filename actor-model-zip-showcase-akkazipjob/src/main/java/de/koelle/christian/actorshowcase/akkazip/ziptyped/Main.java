package de.koelle.christian.actorshowcase.akkazip.ziptyped;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import de.koelle.christian.actorshowcase.akkazip.ziptyped.commands.Command;
import de.koelle.christian.actorshowcase.akkazip.ziptyped.commands.ZipJobActorRequest;
import de.koelle.christian.actorshowcase.common.TestdataSupplier;

import java.nio.file.Paths;

public class Main {

    private final TestdataSupplier testdataSupplier = new TestdataSupplier();

    public static void main(String[] args) {
     new Main().doSomething();
    }

    private void doSomething() {
        var inputZipJob = testdataSupplier.createHierarchicalZipJob(Paths.get("target/zips"), "myZipJob", 2, 2, false);
        ActorRef<Command> testSystem = ActorSystem.create(ZipMainActor.create(AkkaAppIds.ZIP_MAIN_ACTOR_TIMEOUT), "zip-job-execution");
        testSystem.tell(new ZipJobActorRequest(inputZipJob));
    }

}
