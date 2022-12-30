package de.koelle.christian.actorshowcase.akkazip;

import akka.actor.ActorRef;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import de.koelle.christian.actorshowcase.akkazip.nonakka.OurFileUtils;
import de.koelle.christian.actorshowcase.akkazip.nonakka.TestdataSupplier;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SupervisorActor extends UntypedAbstractActor {

    private final OurFileUtils ourFileUtils = new OurFileUtils();
    private final TestdataSupplier testdataSupplier = new TestdataSupplier();
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private ActorRef zipActor;

    private Map<String, Set<ZipJobResult>> interimResults = new LinkedHashMap();

    public SupervisorActor(ActorRef zipActor) {
        this.zipActor = zipActor;
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof ZipJobResult zipJobResult) {
            log.info("{}: {}", zipJobResult.job().jobName(), zipJobResult.tempResultPath());
        } else {
            final Map<String, Map<String, Set<Path>>> partitionedExampleFilePaths = testdataSupplier.getPartitionedExampleFilePaths();

            for (Map.Entry<String, Map<String, Set<Path>>> level1 : partitionedExampleFilePaths.entrySet()) {
                for (Map.Entry<String, Set<Path>> level2 : level1.getValue().entrySet()) {
                    String jobDescription = "%s_%s".formatted(level1.getKey(), level2.getKey());

                    zipActor.tell(new ZipJob(jobDescription, level2.getValue()), getSelf());
                }
            }
        }
    }


}
