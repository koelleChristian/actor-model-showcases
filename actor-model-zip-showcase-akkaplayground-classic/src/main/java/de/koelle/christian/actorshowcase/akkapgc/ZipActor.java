package de.koelle.christian.actorshowcase.akkapgc;

import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import de.koelle.christian.actorshowcase.common.Preconditions;
import de.koelle.christian.actorshowcase.common.TechnicalZipper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class ZipActor extends UntypedAbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final TechnicalZipper technicalZipper = new TechnicalZipper();

    @Override
    public void onReceive(Object message) throws Throwable {
        log.info("Received Message: " + message);

        if (message instanceof ZipJob zipJob) {
            final var things2BZipped = zipJob
                    .filePathsToBeZipped()
                    .stream()
                    .collect(Collectors.toMap(
                            i -> String.valueOf(i.getFileName()),
                            this::getInputStreamSupplierByFilePath
                    ));
            final Path zipAsTemporaryFile = technicalZipper.createZipAsTemporaryFile(things2BZipped);
            getSender().tell(new ZipJobResult(zipJob, zipAsTemporaryFile), getSelf());
        }
    }

    TechnicalZipper.ZipInputStreamSupplier getInputStreamSupplierByFilePath(final Path filePath) {
        Preconditions.checkNotNull(filePath);
        return () -> {
            if (Files.exists(filePath)) {
                try {
                    return Files.newInputStream(filePath);
                } catch (final IOException exception) {
                    throw new IllegalStateException("whatever", exception);
                }
            }
            return null;
        };
    }
}
