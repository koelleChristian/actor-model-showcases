/*
 * Copyright 2022 IQTIG – Institut für Qualitätssicherung und Transparenz im Gesundheitswesen.
 * Dieser Code ist urheberrechtlich geschützt (Copyright). Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, beim IQTIG.
 * Wer gegen das Urheberrecht verstößt, macht sich gem. § 106 ff Urhebergesetz strafbar. Er wird zudem kostenpflichtig abgemahnt und muss
 * Schadensersatz leisten.
 */
package de.koelle.christian.actorshowcase.akkazip.ziptyped;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.LoggingTestKit;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import de.koelle.christian.actorshowcase.akkazip.nonakka.TestdataSupplier;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ZipExecutionActorTest {

    static final ActorTestKit testKit = ActorTestKit.create();

    @AfterAll
    static void cleanup() {
        testKit.shutdownTestKit();
    }

//    @ClassRule
//    public static final TestKitJunitResource testKit = new TestKitJunitResource();

    private final TestdataSupplier testdataSupplier = new TestdataSupplier();

    @Test
    public void testZippingPositive() {
        TestProbe<ZipExecutionActor.ZipExecutionActorResponse> probe = testKit.createTestProbe(ZipExecutionActor.ZipExecutionActorResponse.class);
        ActorRef<ZipExecutionActor.Command> actor = testKit.spawn(ZipExecutionActor.create());

        final String zipJobName = "myZipJob";
        final var filePathsToBeZippedByTargetName = testdataSupplier
                .getExampleFilePaths()
                .stream()
                .limit(2) // Only two real files for this test.
                .collect(Collectors.toMap(
                        i -> i.getFileName().toString(),
                        i -> i
                ));
        actor.tell(new ZipExecutionActor.ZipExcecutionActorRequest(zipJobName, filePathsToBeZippedByTargetName, probe.getRef()));
        ZipExecutionActor.ZipExecutionActorResponse response = probe.receiveMessage();

        Assertions.assertEquals(zipJobName, response.job().jobName());
        Assertions.assertEquals(filePathsToBeZippedByTargetName, response.job().filePathsToBeZippedByTargetName());
        Assertions.assertNotNull(response.tempResultPath());
    }

    @Test
    public void testZippingNegativeFileUnavailable1() {
        TestProbe<ZipExecutionActor.ZipExecutionActorResponse> probe = testKit.createTestProbe(ZipExecutionActor.ZipExecutionActorResponse.class);
        ActorRef<ZipExecutionActor.Command> actor = testKit.spawn(ZipExecutionActor.create());

        final String zipJobName = "myZipJob";
        final Map<String, Path> filePathsToBeZippedByTargetName = Map.of("a.jpg", Paths.get("target", UUID.randomUUID().toString() + ".jpeg"));
        actor.tell(new ZipExecutionActor.ZipExcecutionActorRequest(zipJobName, filePathsToBeZippedByTargetName, probe.getRef()));
        probe.expectNoMessage();
    }

    @Test
    public void testZippingNegativeFileUnavailable2() {
        TestProbe<ZipExecutionActor.ZipExecutionActorResponse> probe = testKit.createTestProbe(ZipExecutionActor.ZipExecutionActorResponse.class);
        ActorRef<ZipExecutionActor.Command> actor = testKit.spawn(ZipExecutionActor.create());

        final String zipJobName = "myZipJob";
        final Map<String, Path> filePathsToBeZippedByTargetName = Map.of("a.jpg", Paths.get("target", UUID.randomUUID().toString() + ".jpeg"));
        LoggingTestKit.error(IllegalStateException.class)
                .withMessageRegex(".*Error as file to be zipped is not available.*")
                .withOccurrences(1)
                .expect(
                        testKit.system(),
                        () -> {
                            actor.tell((new ZipExecutionActor.ZipExcecutionActorRequest(zipJobName, filePathsToBeZippedByTargetName, probe.getRef())));
                            return null;
                        });
    }
}
