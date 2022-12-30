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
import de.koelle.christian.actorshowcase.akkazip.tp02.DeviceManager.*;
import de.koelle.christian.actorshowcase.akkazip.tp02.TemperatureReadingObjs.*;
import org.junit.ClassRule;
import org.junit.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class DeviceGroupTest {

    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void testReplyToRegistrationRequests() {
        TestProbe<DeviceRegistered> probe = testKit.createTestProbe(DeviceRegistered.class);
        ActorRef<DeviceGroup.Command> groupActor = testKit.spawn(DeviceGroup.create("group"));

        groupActor.tell(new RequestTrackDevice("group", "device1", probe.getRef()));
        DeviceRegistered registered1 = probe.receiveMessage();

        // another deviceId
        groupActor.tell(new RequestTrackDevice("group", "device2", probe.getRef()));
        DeviceRegistered registered2 = probe.receiveMessage();
        assertNotEquals(registered1.device(), registered2.device());

        // Check that the device actors are working
        TestProbe<Device.TemperatureRecorded> recordProbe = testKit.createTestProbe(Device.TemperatureRecorded.class);
        registered1.device().tell(new Device.RecordTemperature(0L, 1.0, recordProbe.getRef()));
        assertEquals(0L, recordProbe.receiveMessage().requestId());
        registered2.device().tell(new Device.RecordTemperature(1L, 2.0, recordProbe.getRef()));
        assertEquals(1L, recordProbe.receiveMessage().requestId());
    }

    @Test
    public void testReturnSameActorForSameDeviceId() {
        TestProbe<DeviceRegistered> probe = testKit.createTestProbe(DeviceRegistered.class);
        ActorRef<DeviceGroup.Command> groupActor = testKit.spawn(DeviceGroup.create("group"));

        groupActor.tell(new RequestTrackDevice("group", "device", probe.getRef()));
        DeviceRegistered registered1 = probe.receiveMessage();

        // registering same again should be idempotent
        groupActor.tell(new RequestTrackDevice("group", "device", probe.getRef()));
        DeviceRegistered registered2 = probe.receiveMessage();
        assertEquals(registered1.device(), registered2.device());
    }

    @Test
    public void testIgnoreWrongRegistrationRequests() {
        TestProbe<DeviceRegistered> probe = testKit.createTestProbe(DeviceRegistered.class);
        ActorRef<DeviceGroup.Command> groupActor = testKit.spawn(DeviceGroup.create("group"));
        groupActor.tell(new RequestTrackDevice("wrongGroup", "device1", probe.getRef()));
        probe.expectNoMessage();
    }

    @Test
    public void testListActiveDevices() {
        TestProbe<DeviceRegistered> registeredProbe = testKit.createTestProbe(DeviceRegistered.class);
        ActorRef<DeviceGroup.Command> groupActor = testKit.spawn(DeviceGroup.create("group"));

        groupActor.tell(new RequestTrackDevice("group", "device1", registeredProbe.getRef()));
        registeredProbe.receiveMessage();

        groupActor.tell(new RequestTrackDevice("group", "device2", registeredProbe.getRef()));
        registeredProbe.receiveMessage();

        TestProbe<ReplyDeviceList> deviceListProbe = testKit.createTestProbe(ReplyDeviceList.class);

        groupActor.tell(new RequestDeviceList(0L, "group", deviceListProbe.getRef()));
        ReplyDeviceList reply = deviceListProbe.receiveMessage();
        assertEquals(0L, reply.requestId());
        assertEquals(Stream.of("device1", "device2").collect(Collectors.toSet()), reply.ids());
    }

    @Test
    public void testListActiveDevicesAfterOneShutsDown() {
        TestProbe<DeviceRegistered> registeredProbe = testKit.createTestProbe(DeviceRegistered.class);
        ActorRef<DeviceGroup.Command> groupActor = testKit.spawn(DeviceGroup.create("group"));

        groupActor.tell(new RequestTrackDevice("group", "device1", registeredProbe.getRef()));
        DeviceRegistered registered1 = registeredProbe.receiveMessage();

        groupActor.tell(new RequestTrackDevice("group", "device2", registeredProbe.getRef()));
        DeviceRegistered registered2 = registeredProbe.receiveMessage();

        ActorRef<Device.Command> toShutDown = registered1.device();

        TestProbe<ReplyDeviceList> deviceListProbe = testKit.createTestProbe(ReplyDeviceList.class);

        groupActor.tell(new RequestDeviceList(0L, "group", deviceListProbe.getRef()));
        ReplyDeviceList reply = deviceListProbe.receiveMessage();
        assertEquals(0L, reply.requestId());
        assertEquals(Stream.of("device1", "device2").collect(Collectors.toSet()), reply.ids());

        toShutDown.tell(Device.Passivate.INSTANCE);
        registeredProbe.expectTerminated(toShutDown, registeredProbe.getRemainingOrDefault());

        // using awaitAssert to retry because it might take longer for the groupActor
        // to see the Terminated, that order is undefined
        registeredProbe.awaitAssert(
                () -> {
                    groupActor.tell(new RequestDeviceList(1L, "group", deviceListProbe.getRef()));
                    ReplyDeviceList r = deviceListProbe.receiveMessage();
                    assertEquals(1L, r.requestId());
                    assertEquals(Stream.of("device2").collect(Collectors.toSet()), r.ids());
                    return null;
                });
    }

    @Test
    public void testReturnTemperatureValueForWorkingDevices() {
        TestProbe<RespondAllTemperatures> requester =
                testKit.createTestProbe(RespondAllTemperatures.class);
        TestProbe<Device.Command> device1 = testKit.createTestProbe(Device.Command.class);
        TestProbe<Device.Command> device2 = testKit.createTestProbe(Device.Command.class);

        Map<String, ActorRef<Device.Command>> deviceIdToActor = new HashMap<>();
        deviceIdToActor.put("device1", device1.getRef());
        deviceIdToActor.put("device2", device2.getRef());

        ActorRef<DeviceGroupQuery.Command> queryActor =
                testKit.spawn(
                        DeviceGroupQuery.create(
                                deviceIdToActor, 1L, requester.getRef(), Duration.ofSeconds(3)));

        device1.expectMessageClass(Device.ReadTemperature.class);
        device2.expectMessageClass(Device.ReadTemperature.class);

        queryActor.tell(
                new DeviceGroupQuery.WrappedRespondTemperature(
                        new Device.RespondTemperature(0L, "device1", Optional.of(1.0))));

        queryActor.tell(
                new DeviceGroupQuery.WrappedRespondTemperature(
                        new Device.RespondTemperature(0L, "device2", Optional.of(2.0))));

        RespondAllTemperatures response = requester.receiveMessage();
        assertEquals(1L, response.requestId);

        Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new TemperatureValue(1.0));
        expectedTemperatures.put("device2", new TemperatureValue(2.0));

        assertEquals(expectedTemperatures, response.temperatures);
    }

    @Test
    public void testReturnTemperatureNotAvailableForDevicesWithNoReadings() {
        TestProbe<RespondAllTemperatures> requester =
                testKit.createTestProbe(RespondAllTemperatures.class);
        TestProbe<Device.Command> device1 = testKit.createTestProbe(Device.Command.class);
        TestProbe<Device.Command> device2 = testKit.createTestProbe(Device.Command.class);

        Map<String, ActorRef<Device.Command>> deviceIdToActor = new HashMap<>();
        deviceIdToActor.put("device1", device1.getRef());
        deviceIdToActor.put("device2", device2.getRef());

        ActorRef<DeviceGroupQuery.Command> queryActor =
                testKit.spawn(
                        DeviceGroupQuery.create(
                                deviceIdToActor, 1L, requester.getRef(), Duration.ofSeconds(3)));

        assertEquals(0L, device1.expectMessageClass(Device.ReadTemperature.class).requestId());
        assertEquals(0L, device2.expectMessageClass(Device.ReadTemperature.class).requestId());

        queryActor.tell(
                new DeviceGroupQuery.WrappedRespondTemperature(
                        new Device.RespondTemperature(0L, "device1", Optional.empty())));

        queryActor.tell(
                new DeviceGroupQuery.WrappedRespondTemperature(
                        new Device.RespondTemperature(0L, "device2", Optional.of(2.0))));

        RespondAllTemperatures response = requester.receiveMessage();
        assertEquals(1L, response.requestId);

        Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", TemperatureNotAvailable.INSTANCE);
        expectedTemperatures.put("device2", new TemperatureValue(2.0));

        assertEquals(expectedTemperatures, response.temperatures);
    }
    @Test
    public void testReturnDeviceNotAvailableIfDeviceStopsBeforeAnswering() {
        TestProbe<RespondAllTemperatures> requester =
                testKit.createTestProbe(RespondAllTemperatures.class);
        TestProbe<Device.Command> device1 = testKit.createTestProbe(Device.Command.class);
        TestProbe<Device.Command> device2 = testKit.createTestProbe(Device.Command.class);

        Map<String, ActorRef<Device.Command>> deviceIdToActor = new HashMap<>();
        deviceIdToActor.put("device1", device1.getRef());
        deviceIdToActor.put("device2", device2.getRef());

        ActorRef<DeviceGroupQuery.Command> queryActor =
                testKit.spawn(
                        DeviceGroupQuery.create(
                                deviceIdToActor, 1L, requester.getRef(), Duration.ofSeconds(3)));

        assertEquals(0L, device1.expectMessageClass(Device.ReadTemperature.class).requestId());
        assertEquals(0L, device2.expectMessageClass(Device.ReadTemperature.class).requestId());

        queryActor.tell(
                new DeviceGroupQuery.WrappedRespondTemperature(
                        new Device.RespondTemperature(0L, "device1", Optional.of(1.0))));

        device2.stop();

        RespondAllTemperatures response = requester.receiveMessage();
        assertEquals(1L, response.requestId);

        Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new TemperatureValue(1.0));
        expectedTemperatures.put("device2", DeviceNotAvailable.INSTANCE);

        assertEquals(expectedTemperatures, response.temperatures);
    }

    @Test
    public void testReturnTemperatureReadingEvenIfDeviceStopsAfterAnswering() {
        TestProbe<RespondAllTemperatures> requester =
                testKit.createTestProbe(RespondAllTemperatures.class);
        TestProbe<Device.Command> device1 = testKit.createTestProbe(Device.Command.class);
        TestProbe<Device.Command> device2 = testKit.createTestProbe(Device.Command.class);

        Map<String, ActorRef<Device.Command>> deviceIdToActor = new HashMap<>();
        deviceIdToActor.put("device1", device1.getRef());
        deviceIdToActor.put("device2", device2.getRef());

        ActorRef<DeviceGroupQuery.Command> queryActor =
                testKit.spawn(
                        DeviceGroupQuery.create(
                                deviceIdToActor, 1L, requester.getRef(), Duration.ofSeconds(3)));

        assertEquals(0L, device1.expectMessageClass(Device.ReadTemperature.class).requestId());
        assertEquals(0L, device2.expectMessageClass(Device.ReadTemperature.class).requestId());

        queryActor.tell(
                new DeviceGroupQuery.WrappedRespondTemperature(
                        new Device.RespondTemperature(0L, "device1", Optional.of(1.0))));

        queryActor.tell(
                new DeviceGroupQuery.WrappedRespondTemperature(
                        new Device.RespondTemperature(0L, "device2", Optional.of(2.0))));

        device2.stop();

        RespondAllTemperatures response = requester.receiveMessage();
        assertEquals(1L, response.requestId);

        Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new TemperatureValue(1.0));
        expectedTemperatures.put("device2", new TemperatureValue(2.0));

        assertEquals(expectedTemperatures, response.temperatures);
    }
    @Test
    public void testReturnDeviceTimedOutIfDeviceDoesNotAnswerInTime() {
        TestProbe<RespondAllTemperatures> requester =
                testKit.createTestProbe(RespondAllTemperatures.class);
        TestProbe<Device.Command> device1 = testKit.createTestProbe(Device.Command.class);
        TestProbe<Device.Command> device2 = testKit.createTestProbe(Device.Command.class);

        Map<String, ActorRef<Device.Command>> deviceIdToActor = new HashMap<>();
        deviceIdToActor.put("device1", device1.getRef());
        deviceIdToActor.put("device2", device2.getRef());

        ActorRef<DeviceGroupQuery.Command> queryActor =
                testKit.spawn(
                        DeviceGroupQuery.create(
                                deviceIdToActor, 1L, requester.getRef(), Duration.ofMillis(200)));

        assertEquals(0L, device1.expectMessageClass(Device.ReadTemperature.class).requestId());
        assertEquals(0L, device2.expectMessageClass(Device.ReadTemperature.class).requestId());

        queryActor.tell(
                new DeviceGroupQuery.WrappedRespondTemperature(
                        new Device.RespondTemperature(0L, "device1", Optional.of(1.0))));

        // no reply from device2

        RespondAllTemperatures response = requester.receiveMessage();
        assertEquals(1L, response.requestId);

        Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new TemperatureValue(1.0));
        expectedTemperatures.put("device2", DeviceTimedOut.INSTANCE);

        assertEquals(expectedTemperatures, response.temperatures);
    }

    @Test
    public void testCollectTemperaturesFromAllActiveDevices() {
        TestProbe<DeviceRegistered> registeredProbe = testKit.createTestProbe(DeviceRegistered.class);
        ActorRef<DeviceGroup.Command> groupActor = testKit.spawn(DeviceGroup.create("group"));

        groupActor.tell(new RequestTrackDevice("group", "device1", registeredProbe.getRef()));
        ActorRef<Device.Command> deviceActor1 = registeredProbe.receiveMessage().device();

        groupActor.tell(new RequestTrackDevice("group", "device2", registeredProbe.getRef()));
        ActorRef<Device.Command> deviceActor2 = registeredProbe.receiveMessage().device();

        groupActor.tell(new RequestTrackDevice("group", "device3", registeredProbe.getRef()));
        ActorRef<Device.Command> deviceActor3 = registeredProbe.receiveMessage().device();

        // Check that the device actors are working
        TestProbe<Device.TemperatureRecorded> recordProbe =
                testKit.createTestProbe(Device.TemperatureRecorded.class);
        deviceActor1.tell(new Device.RecordTemperature(0L, 1.0, recordProbe.getRef()));
        assertEquals(0L, recordProbe.receiveMessage().requestId());
        deviceActor2.tell(new Device.RecordTemperature(1L, 2.0, recordProbe.getRef()));
        assertEquals(1L, recordProbe.receiveMessage().requestId());
        // No temperature for device 3

        TestProbe<RespondAllTemperatures> allTempProbe =
                testKit.createTestProbe(RespondAllTemperatures.class);
        groupActor.tell(new RequestAllTemperatures(0L, "group", allTempProbe.getRef()));
        RespondAllTemperatures response = allTempProbe.receiveMessage();
        assertEquals(0L, response.requestId);

        Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new TemperatureValue(1.0));
        expectedTemperatures.put("device2", new TemperatureValue(2.0));
        expectedTemperatures.put("device3", TemperatureNotAvailable.INSTANCE);

        assertEquals(expectedTemperatures, response.temperatures);
    }
}
