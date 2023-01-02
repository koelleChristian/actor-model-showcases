/*
 * Copyright 2022 IQTIG – Institut für Qualitätssicherung und Transparenz im Gesundheitswesen.
 * Dieser Code ist urheberrechtlich geschützt (Copyright). Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, beim IQTIG.
 * Wer gegen das Urheberrecht verstößt, macht sich gem. § 106 ff Urhebergesetz strafbar. Er wird zudem kostenpflichtig abgemahnt und muss
 * Schadensersatz leisten.
 */
package de.koelle.christian.actorshowcase.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;
import java.util.Set;

class TestdataSupplierTest {

    @CsvSource({
            "0,  2, false",
            "2,  2, true",
            "1, 10, true",
            "10, 1, false",
            "5,  2, true",
            "6,  2, false",
            "7,  2, true",
            "6,  6, false",
            "0,  0, true"
    })
    @ParameterizedTest
    void testCreateHierarchicalZipJob(int depth, int width, boolean persistSubZips) {
        final ZipJob hierarchicalZipJob = new TestdataSupplier()
                .createHierarchicalZipJob(Path.of("target"), "myZip", depth, width, persistSubZips);
        Assertions.assertNotNull(hierarchicalZipJob);
        System.out.println(hierarchicalZipJob.toStringHierarchical());
    }

    @Test
    void testGetExampleFilePaths() {
        final Set<Path> exampleFilePaths = new TestdataSupplier().getExampleFilePaths(FileClass.IMAGES);
        Assertions.assertNotNull(exampleFilePaths);
        for (Path exampleFilePath : exampleFilePaths) {
            System.out.println(exampleFilePath);
        }
    }
}
