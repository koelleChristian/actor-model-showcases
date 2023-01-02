package de.koelle.christian.actorshowcase.akkazip.ziptyped;

import akka.actor.DeadLetter;
import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import de.koelle.christian.actorshowcase.akkazip.ziptyped.commands.Command;
import de.koelle.christian.actorshowcase.akkazip.ziptyped.commands.ZipJobActorRequest;
import de.koelle.christian.actorshowcase.akkazip.ziptyped.commands.ZipJobActorResponse;
import de.koelle.christian.actorshowcase.common.OurFileUtils;
import de.koelle.christian.actorshowcase.common.TestdataSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

public class ZipMainActorTest {
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
    public void testPositive() {
        var inputZipJob = testdataSupplier.createHierarchicalZipJob(Paths.get("target/zips"), "myZipJob", 0, 2, false);
        final Duration timeout = AkkaAppIds.ZIP_MAIN_ACTOR_TIMEOUT;

        TestProbe<ZipJobActorResponse> probeMsg = TEST_KIT.createTestProbe(ZipJobActorResponse.class);
        TestProbe<DeadLetter> probeDeadLetter = TEST_KIT.createDeadLetterProbe();
        final ActorRef<Command> actor = TEST_KIT.spawn(ZipMainActor.create(timeout));

        actor.tell(new ZipJobActorRequest(inputZipJob));
//        DeadLetter responseMsg = probe2.receiveMessage();
//        DeadLetter responseDeadLetter = probe2.receiveMessage();

//        var actualTempResultPath = response.zipExecutionActorResponse().tempResultPath();
//        var actualJobName = response.zipExecutionActorResponse().job().jobName();
//        var actualBinaryInput = response.zipExecutionActorResponse().job().zipInputStreamSupplierByTargetFileName();
//
//        Assertions.assertEquals(inputZipJob.targetZipFileName(), actualJobName);
//        Assertions.assertEquals(inputZipJob.filesToBeIncluded(), actualBinaryInput);
//        Assertions.assertNotNull(actualTempResultPath);
//
//        ourFileUtils.moveFile(actualTempResultPath, Paths.get(
//                "target",
//                "test",
//                "%s_testPositive_%s.zip".formatted(this.getClass().getSimpleName(), sortableTimeStampFormatter.format(LocalDateTime.now()))));
    }
}
