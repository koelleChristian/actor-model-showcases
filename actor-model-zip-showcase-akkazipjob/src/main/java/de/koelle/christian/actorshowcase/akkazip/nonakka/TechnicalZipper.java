/*
 * Copyright 2018 IQTIG – Institut für Qualitätssicherung und Transparenz im Gesundheitswesen.
 * Dieser Code ist urheberrechtlich geschützt (Copyright). Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, beim IQTIG.
 * Wer gegen das Urheberrecht verstößt, macht sich gem. § 106 ff Urhebergesetz strafbar. Er wird zudem kostenpflichtig abgemahnt und muss
 * Schadensersatz leisten.
 */
package de.koelle.christian.actorshowcase.akkazip.nonakka;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Provides the technical zipping without any domain knowledge.
 */
public class TechnicalZipper {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @FunctionalInterface
    public interface ZipInputStreamSupplier {

        InputStream get();
    }

    /**
     * Creates a temporary zip-file, with binary data provided by the ZipInputStreamSupplier.
     *
     * @param zipableFilesByFileNameSupplier binary data suppliers by the file name to be used within the zip-file.
     * @return Path to the temporary zip-file created.
     */
    public Path createZipAsTemporaryFile(final Map<String, ZipInputStreamSupplier> zipableFilesByFileNameSupplier) {
        Preconditions.checkArgument(zipableFilesByFileNameSupplier != null);
        final Path tempFolderPath = OurFileUtils.getTempFolderPathWithFallbackToCurrentFolder();
        Path zipTmpFilePath;
        try {
            zipTmpFilePath = Files.createTempFile(tempFolderPath, null, ".tmp");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        try (BufferedOutputStream bufferedTmpFileOutputStream = new BufferedOutputStream(new FileOutputStream(zipTmpFilePath.toFile()))) {
            createZipFileActually(bufferedTmpFileOutputStream, zipableFilesByFileNameSupplier);
            return zipTmpFilePath;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("squid:S4087")//"S4087 close()" calls should not be redundant
    private void createZipFileActually(final OutputStream outputStream, final Map<String, ZipInputStreamSupplier> filesToAddAsName2InputStreamSupplierMap) {
        try (final ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            if (filesToAddAsName2InputStreamSupplierMap != null) {
                filesToAddAsName2InputStreamSupplierMap.forEach(
                        (k, v) -> addZipEntryUnlessNoBytes(zos, k, v.get())
                );
            }
            //noinspection RedundantExplicitClose
            zos.close();// close explicitly despite auto closing
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void addZipEntryUnlessNoBytes(final ZipOutputStream zipOutputStreamTarget, final String binaryDataToAddFileName, final InputStream binaryDataToAdd) {
        try {
            if (binaryDataToAdd != null && binaryDataToAdd.available() > 0) {
                // Anmerkung: Was nicht vorhanden ist, wird in den Suppliern für die die Binärdaten geloggt.
                final ZipEntry zipEntry = new ZipEntry(binaryDataToAddFileName);
                zipOutputStreamTarget.putNextEntry(zipEntry);

                byte[] buffer = new byte[AppIds.BUFFERSIZE];
                int read;
                while ((read = binaryDataToAdd.read(buffer)) != -1) {
                    zipOutputStreamTarget.write(buffer, 0, read);
                }
                zipOutputStreamTarget.closeEntry();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                if (binaryDataToAdd != null) {
                    binaryDataToAdd.close();
                }
            } catch (IOException e) {
                log.info("Error on closing the input stream ", e);
            }
        }
    }


}