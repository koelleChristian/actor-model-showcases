package de.koelle.christian.actorshowcase.akkazip;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

public class Main {

    public static void main(String[] args) {
        ActorSystem actorSystem = ActorSystem.create();
        ActorRef zipActor = actorSystem.actorOf(Props.create(ZipActor.class), "ziptyped-actor");
        ActorRef supervisorActor = actorSystem.actorOf(Props.create(SupervisorActor.class, zipActor), "supervisor");
        supervisorActor.tell("start", supervisorActor); // Message cannot be null
    }
}