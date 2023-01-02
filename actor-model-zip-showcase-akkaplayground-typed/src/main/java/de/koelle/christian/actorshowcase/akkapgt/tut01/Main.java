package de.koelle.christian.actorshowcase.akkapgt.tut01;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;


public class Main {
  public static void main(String[] args) {
    ActorRef<String> testSystem = ActorSystem.create(MainActor.create(), "testSystem");
    testSystem.tell("start");
  }
}