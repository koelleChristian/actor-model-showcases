/*
 * Copyright 2022 IQTIG – Institut für Qualitätssicherung und Transparenz im Gesundheitswesen.
 * Dieser Code ist urheberrechtlich geschützt (Copyright). Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, beim IQTIG.
 * Wer gegen das Urheberrecht verstößt, macht sich gem. § 106 ff Urhebergesetz strafbar. Er wird zudem kostenpflichtig abgemahnt und muss
 * Schadensersatz leisten.
 */
package de.koelle.christian.actorshowcase.akkazip.nonakka;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

class TestdataSupplierTest {

    @Test
    void testTestdataCreation() {
        final ZipJob hierarchicalZipJob = new TestdataSupplier().getHierarchicalZipJob(Path.of("target"), "myZip");
        Assertions.assertNotNull(hierarchicalZipJob);
        System.out.println(hierarchicalZipJob);
    }
}
