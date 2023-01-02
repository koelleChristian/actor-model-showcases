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
import de.koelle.christian.actorshowcase.akkazip.ziptyped.commands.Command;
import de.koelle.christian.actorshowcase.akkazip.ziptyped.commands.ZipExcecutionActorRequest;
import de.koelle.christian.actorshowcase.akkazip.ziptyped.commands.ZipExecutionActorResponse;
import de.koelle.christian.actorshowcase.common.FileClass;
import de.koelle.christian.actorshowcase.common.OurFileUtils;
import de.koelle.christian.actorshowcase.common.TechnicalZipperSupport;
import de.koelle.christian.actorshowcase.common.TestdataSupplier;
import de.koelle.christian.actorshowcase.common.ZipInputStreamSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ZipExecutionActorTest {

    private static final ActorTestKit TEST_KIT = ActorTestKit.create();
    // public static final TestKitJunitResource TEST_KIT = new TestKitJunitResource(); // I am unwilling to use JUnit 4 again.
    private final TestdataSupplier testdataSupplier = new TestdataSupplier();
    private final OurFileUtils ourFileUtils = new OurFileUtils();
    private final DateTimeFormatter sortableTimeStampFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @AfterAll
    static void cleanup() {
        TEST_KIT.shutdownTestKit();
    }

    @Test
    public void testZippingPositive() {
        TestProbe<ZipExecutionActorResponse> probe = TEST_KIT.createTestProbe(ZipExecutionActorResponse.class);
        ActorRef<Command> actor = TEST_KIT.spawn(ZipExecutionActor.create());

        final String zipJobName = "myZipJob";
        final Map<String, ZipInputStreamSupplier> filePathsToBeZippedByTargetName = testdataSupplier
                .getExampleFilePaths(FileClass.IMAGES)
                .stream()
                .limit(2) // Only two real files for this test.
                .collect(Collectors.toMap(
                        i -> i.getFileName().toString(),
                        TechnicalZipperSupport::getIssForFilePath
                ));
        actor.tell(new ZipExcecutionActorRequest(zipJobName, filePathsToBeZippedByTargetName, probe.getRef()));
        ZipExecutionActorResponse response = probe.receiveMessage();

        var actualJobName = response.job().jobName();
        var actualBinaryInput = response.job().zipInputStreamSupplierByTargetFileName();
        var actualTempResultPath = response.tempResultPath();

        Assertions.assertEquals(zipJobName, actualJobName);
        Assertions.assertEquals(filePathsToBeZippedByTargetName, actualBinaryInput);
        Assertions.assertNotNull(actualTempResultPath);

        ourFileUtils.moveFile(actualTempResultPath, Paths.get(
                "target",
                "test",
                "%s_testZippingPositive_%s.zip".formatted(this.getClass().getSimpleName(), sortableTimeStampFormatter.format(LocalDateTime.now()))));
    }

    @Test
    public void testZippingNegativeFileUnavailableWithoutAssertion() {
        TestProbe<ZipExecutionActorResponse> probe = TEST_KIT.createTestProbe(ZipExecutionActorResponse.class);
        ActorRef<Command> actor = TEST_KIT.spawn(ZipExecutionActor.create());

        final String zipJobName = "myZipJob";
        final Map<String, ZipInputStreamSupplier> filePathsToBeZippedByTargetName = Map.of(
                "a.jpg",
                TechnicalZipperSupport.getIssForFilePath(Paths.get("target", UUID.randomUUID().toString() + ".jpeg"))
        );
        actor.tell(new ZipExcecutionActorRequest(zipJobName, filePathsToBeZippedByTargetName, probe.getRef()));
        probe.expectNoMessage();
    }

    @Test
    public void testZippingNegativeFileUnavailableWithErrorAssertion() {
        TestProbe<ZipExecutionActorResponse> probe = TEST_KIT.createTestProbe(ZipExecutionActorResponse.class);
        ActorRef<Command> actor = TEST_KIT.spawn(ZipExecutionActor.create());

        final String zipJobName = "myZipJob";
        final Map<String, ZipInputStreamSupplier> filePathsToBeZippedByTargetName = Map.of(
                "a.jpg",
                TechnicalZipperSupport.getIssForFilePath(Paths.get("target", UUID.randomUUID().toString() + ".jpeg"))
        );

        LoggingTestKit.error(IllegalStateException.class)
                .withMessageRegex(".*Error as file to be zipped is not available.*")
                .withOccurrences(1)
                .expect(
                        TEST_KIT.system(),
                        () -> {
                            actor.tell((new ZipExcecutionActorRequest(zipJobName, filePathsToBeZippedByTargetName, probe.getRef())));
                            return null;
                        });
    }
}
