package de.koelle.christian.actorshowcase.akkazip.ziptyped;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import de.koelle.christian.actorshowcase.akkazip.nonakka.TestdataSupplier;
import org.junit.jupiter.api.AfterAll;

public class ZipJobActorTest {
    static final ActorTestKit testKit = ActorTestKit.create();

    private final TestdataSupplier testdataSupplier = new TestdataSupplier();


    @AfterAll
    static void cleanup() {
        testKit.shutdownTestKit();
    }

    public void testPositive() {
//        TestProbe<ZipJobActor.ZipJobActorResponse> probe = testKit.createTestProbe(ZipJobActor.ZipJobActorResponse.class);
//        ActorRef<ZipJobActor.Command> actor = testKit.spawn(ZipJobActor.create());
//
//        final String zipJobName = "myZipJob";
//        final var filePathsToBeZippedByTargetName = testdataSupplier
//                .getExampleFilePaths()
//                .stream()
//                .limit(2) // Only two real files for this test.
//                .collect(Collectors.toMap(
//                        i -> i.getFileName().toString(),
//                        i -> i
//                ));
//        actor.tell(new ZipExecutionActor.ZipExcecutionActorRequest(zipJobName, filePathsToBeZippedByTargetName, probe.getRef()));
//        ZipExecutionActor.ZipExecutionActorResponse response = probe.receiveMessage();
//
//        Assertions.assertEquals(zipJobName, response.job().jobName());
//        Assertions.assertEquals(filePathsToBeZippedByTargetName, response.job().filePathsToBeZippedByTargetName());
//        Assertions.assertNotNull(response.tempResultPath());
    }
}
