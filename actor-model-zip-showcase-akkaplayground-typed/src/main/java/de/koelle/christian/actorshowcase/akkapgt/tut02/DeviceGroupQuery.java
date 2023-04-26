package de.koelle.christian.actorshowcase.akkapgt.tut02;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DeviceGroupQuery extends AbstractBehavior<DeviceGroupQuery.Command> {


    public interface Command {
    }

    private static enum CollectionTimeout implements Command {
        INSTANCE
    }

    static class WrappedRespondTemperature implements Command {
        final Device.RespondTemperature response;

        WrappedRespondTemperature(Device.RespondTemperature response) {
            this.response = response;
        }
    }

    private static class DeviceTerminated implements Command {
        final String deviceId;

        private DeviceTerminated(String deviceId) {
            this.deviceId = deviceId;
        }
    }

    public static Behavior<Command> create(
            Map<String, ActorRef<Device.Command>> deviceIdToActor,
            long requestId,
            ActorRef<DeviceManager.RespondAllTemperatures> requester,
            Duration timeout) {
        return Behaviors.setup(
                context ->
                        Behaviors.withTimers(
                                timers ->
                                        new DeviceGroupQuery(
                                                deviceIdToActor, requestId, requester, timeout, context, timers)));
    }

    private final long requestId;
    private final ActorRef<DeviceManager.RespondAllTemperatures> requester;
    private Map<String, TemperatureReadingObjs.TemperatureReading> repliesSoFar = new HashMap<>();
    private final Set<String> stillWaiting;


    private DeviceGroupQuery(
            Map<String, ActorRef<Device.Command>> deviceIdToActor,
            long requestId,
            ActorRef<DeviceManager.RespondAllTemperatures> requester,
            Duration timeout,
            ActorContext<Command> context,
            TimerScheduler<Command> timers) {
        super(context);
        this.requestId = requestId;
        this.requester = requester;

        timers.startSingleTimer(CollectionTimeout.INSTANCE, timeout);

        ActorRef<Device.RespondTemperature> respondTemperatureAdapter =
                context.messageAdapter(Device.RespondTemperature.class, WrappedRespondTemperature::new);

        for (Map.Entry<String, ActorRef<Device.Command>> entry : deviceIdToActor.entrySet()) {
            context.watchWith(entry.getValue(), new DeviceTerminated(entry.getKey()));
            entry.getValue().tell(new Device.ReadTemperature(0L, respondTemperatureAdapter));
        }
        stillWaiting = new HashSet<>(deviceIdToActor.keySet());
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(WrappedRespondTemperature.class, this::onRespondTemperature)
                .onMessage(DeviceTerminated.class, this::onDeviceTerminated)
                .onMessage(CollectionTimeout.class, this::onCollectionTimeout)
                .build();
    }

    private Behavior<Command> onRespondTemperature(WrappedRespondTemperature r) {
        TemperatureReadingObjs.TemperatureReading reading =
                r.response
                        .value()
                        .map(v -> (TemperatureReadingObjs.TemperatureReading) new TemperatureReadingObjs.TemperatureValue(v))
                        .orElse(TemperatureReadingObjs.TemperatureNotAvailable.INSTANCE);

        String deviceId = r.response.deviceId();
        repliesSoFar.put(deviceId, reading);
        stillWaiting.remove(deviceId);

        return respondWhenAllCollected();
    }

    private Behavior<Command> onDeviceTerminated(DeviceTerminated terminated) {
        if (stillWaiting.contains(terminated.deviceId)) {
            repliesSoFar.put(terminated.deviceId, TemperatureReadingObjs.DeviceNotAvailable.INSTANCE);
            stillWaiting.remove(terminated.deviceId);
        }
        return respondWhenAllCollected();
    }

    private Behavior<Command> onCollectionTimeout(CollectionTimeout timeout) {
        for (String deviceId : stillWaiting) {
            repliesSoFar.put(deviceId, TemperatureReadingObjs.DeviceTimedOut.INSTANCE);
        }
        stillWaiting.clear();
        return respondWhenAllCollected();
    }

    private Behavior<Command> respondWhenAllCollected() {
        if (stillWaiting.isEmpty()) {
            requester.tell(new DeviceManager.RespondAllTemperatures(requestId, repliesSoFar));
            return Behaviors.stopped();
        } else {
            return this;
        }
    }

}