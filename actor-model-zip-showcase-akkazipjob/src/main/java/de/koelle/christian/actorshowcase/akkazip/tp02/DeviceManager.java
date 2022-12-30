package de.koelle.christian.actorshowcase.akkazip.tp02;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import de.koelle.christian.actorshowcase.akkazip.tp02.TemperatureReadingObjs.TemperatureReading;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class DeviceManager extends AbstractBehavior<DeviceManager.Command> {

    public interface Command {
    }

    public record RequestTrackDevice(String groupId, String deviceId, ActorRef<DeviceRegistered> replyTo)
            implements DeviceManager.Command, DeviceGroup.Command {
    }

    public record DeviceRegistered(ActorRef<Device.Command> device) {
        // Es wird der Aktor zurückgegeben, der für das registrierte Device zuständig ist.
    }

    public record RequestDeviceList(long requestId, String groupId, ActorRef<ReplyDeviceList> replyTo)
            implements DeviceManager.Command, DeviceGroup.Command {
    }

    public record ReplyDeviceList(long requestId, Set<String> ids) {
    }

    private static class DeviceGroupTerminated implements DeviceManager.Command {
        public final String groupId;

        DeviceGroupTerminated(String groupId) {
            this.groupId = groupId;
        }
    }

    public static final class RequestAllTemperatures
            implements DeviceGroupQuery.Command, DeviceGroup.Command, Command {

        final long requestId;
        final String groupId;
        final ActorRef<RespondAllTemperatures> replyTo;

        public RequestAllTemperatures(
                long requestId, String groupId, ActorRef<RespondAllTemperatures> replyTo) {
            this.requestId = requestId;
            this.groupId = groupId;
            this.replyTo = replyTo;
        }
    }

    public static final class RespondAllTemperatures {
        final long requestId;
        final Map<String, TemperatureReading> temperatures;

        public RespondAllTemperatures(long requestId, Map<String, TemperatureReading> temperatures) {
            this.requestId = requestId;
            this.temperatures = temperatures;
        }
    }



    public static Behavior<Command> create() {
        return Behaviors.setup(DeviceManager::new);
    }

    private final Map<String, ActorRef<DeviceGroup.Command>> groupIdToActor = new HashMap<>();

    private DeviceManager(ActorContext<Command> context) {
        super(context);
        context.getLog().info("DeviceManager started");
    }
    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(RequestTrackDevice.class, this::onTrackDevice)
                .onMessage(RequestDeviceList.class, this::onRequestDeviceList)
                .onMessage(DeviceGroupTerminated.class, this::onTerminated)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }


    private DeviceManager onTrackDevice(RequestTrackDevice trackMsg) {
        String groupId = trackMsg.groupId;
        ActorRef<DeviceGroup.Command> ref = groupIdToActor.get(groupId);
        if (ref != null) {
            ref.tell(trackMsg);
        } else {
            getContext().getLog().info("Creating device group actor for {}", groupId);
            ActorRef<DeviceGroup.Command> groupActor =
                    getContext().spawn(DeviceGroup.create(groupId), "group-" + groupId);
            getContext().watchWith(groupActor, new DeviceGroupTerminated(groupId));
            groupActor.tell(trackMsg);
            groupIdToActor.put(groupId, groupActor);
        }
        return this;
    }

    private DeviceManager onRequestDeviceList(RequestDeviceList request) {
        ActorRef<DeviceGroup.Command> ref = groupIdToActor.get(request.groupId);
        if (ref != null) {
            ref.tell(request);
        } else {
            request.replyTo.tell(new ReplyDeviceList(request.requestId, Collections.emptySet()));
        }
        return this;
    }

    private DeviceManager onTerminated(DeviceGroupTerminated t) {
        getContext().getLog().info("Device group actor for {} has been terminated", t.groupId);
        groupIdToActor.remove(t.groupId);
        return this;
    }


    private DeviceManager onPostStop() {
        getContext().getLog().info("DeviceManager stopped");
        return this;
    }
}