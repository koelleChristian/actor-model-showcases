package de.koelle.christian.actorshowcase.akkapgt.tut01;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class MainActor extends AbstractBehavior<String> {

  static Behavior<String> create() {
    return Behaviors.setup(MainActor::new);
  }

  private MainActor(ActorContext<String> context) {
    super(context);
  }

  @Override
  public Receive<String> createReceive() {
    return newReceiveBuilder().onMessageEquals("start", this::start).build();
  }

  private Behavior<String> start() {
    ActorRef<String> firstRef = getContext().spawn(PrintMyActorRefActor.create(), "first-actor");

    // Teil 1
    System.out.println("First: " + firstRef);
    firstRef.tell("printit");

    // Teil 2 PostStop
    ActorRef<String> first = getContext().spawn(StartStopActor1.create(), "first");
    first.tell("stop");

    // Teil 3 Supervision
    ActorRef<String> supervisingActor = getContext().spawn(SupervisingActor.create(), "supervising-actor");
    supervisingActor.tell("failChild");


    return Behaviors.same();
  }
}