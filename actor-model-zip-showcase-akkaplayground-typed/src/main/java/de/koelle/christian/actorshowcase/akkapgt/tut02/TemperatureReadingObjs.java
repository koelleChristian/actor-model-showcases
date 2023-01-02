package de.koelle.christian.actorshowcase.akkapgt.tut02;

public class TemperatureReadingObjs {
    public interface TemperatureReading {}

    public record TemperatureValue(double value) implements TemperatureReading {

    }

    public enum TemperatureNotAvailable implements TemperatureReading {
        INSTANCE
    }

    public enum DeviceNotAvailable implements TemperatureReading {
        INSTANCE
    }

    public enum DeviceTimedOut implements TemperatureReading {
        INSTANCE
    }
}
