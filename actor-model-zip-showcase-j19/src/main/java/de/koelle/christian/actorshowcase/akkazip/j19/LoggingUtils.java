package de.koelle.christian.actorshowcase.akkazip.j19;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

public class LoggingUtils {

    public void log(Logger logger, Level level, String msg) {
        logger.printf(level, "[%-53s] %s", Thread.currentThread(), msg);
    }
}
