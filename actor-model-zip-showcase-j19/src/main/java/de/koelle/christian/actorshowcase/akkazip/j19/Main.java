package de.koelle.christian.actorshowcase.akkazip.j19;

import de.koelle.christian.actorshowcase.common.TestdataSupplier;
import de.koelle.christian.actorshowcase.common.ZipJob;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Warmup;

import java.nio.file.Path;

public class Main {

    private static final Logger LOG = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        new Main().doMain();
    }

    @Benchmark
    @Fork(value = 1)
    @Warmup(iterations = 2)
    @Measurement(iterations = 1)
    @BenchmarkMode(Mode.All)
    public void doMain() {
        final ZipJob hierarchicalZipJob = new TestdataSupplier()
                .createHierarchicalZipJob(Path.of("target"), "myZipJ19", 3, 2, true);
        LOG.info(hierarchicalZipJob.toStringHierarchical());
        new ZipJobExecutor().doJob(hierarchicalZipJob);
    }
}
