/*
 * Copyright 2022 IQTIG – Institut für Qualitätssicherung und Transparenz im Gesundheitswesen.
 * Dieser Code ist urheberrechtlich geschützt (Copyright). Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, beim IQTIG.
 * Wer gegen das Urheberrecht verstößt, macht sich gem. § 106 ff Urhebergesetz strafbar. Er wird zudem kostenpflichtig abgemahnt und muss
 * Schadensersatz leisten.
 */
package de.koelle.christian.actorshowcase.akkazip.tp02;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.util.Optional;


public class DeviceTest {

    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void testReplyWithEmptyReadingIfNoTemperatureIsKnown() {
        TestProbe<Device.RespondTemperature> probe = testKit.createTestProbe(Device.RespondTemperature.class);
        ActorRef<Device.Command> deviceActor = testKit.spawn(Device.create("group", "device"));
        deviceActor.tell(new Device.ReadTemperature(42L, probe.getRef()));
        Device.RespondTemperature response = probe.receiveMessage();
        Assertions.assertEquals(42L, response.requestId());
        Assertions.assertEquals(Optional.empty(), response.value());
    }
    @Test
    public void testReplyWithLatestTemperatureReading() {
        TestProbe<Device.TemperatureRecorded> recordProbe = testKit.createTestProbe(Device.TemperatureRecorded.class);
        TestProbe<Device.RespondTemperature> readProbe = testKit.createTestProbe(Device.RespondTemperature.class);
        ActorRef<Device.Command> deviceActor = testKit.spawn(Device.create("group", "device"));

        deviceActor.tell(new Device.RecordTemperature(1L, 24.0, recordProbe.getRef()));
        Assertions.assertEquals(1L, recordProbe.receiveMessage().requestId());

        deviceActor.tell(new Device.ReadTemperature(2L, readProbe.getRef()));
        Device.RespondTemperature response1 = readProbe.receiveMessage();
        Assertions.assertEquals(2L, response1.requestId());
        Assertions.assertEquals(Optional.of(24.0), response1.value());

        deviceActor.tell(new Device.RecordTemperature(3L, 55.0, recordProbe.getRef()));
        Assertions.assertEquals(3L, recordProbe.receiveMessage().requestId());

        deviceActor.tell(new Device.ReadTemperature(4L, readProbe.getRef()));
        Device.RespondTemperature response2 = readProbe.receiveMessage();
        Assertions.assertEquals(4L, response2.requestId());
        Assertions.assertEquals(Optional.of(55.0), response2.value());
    }
}