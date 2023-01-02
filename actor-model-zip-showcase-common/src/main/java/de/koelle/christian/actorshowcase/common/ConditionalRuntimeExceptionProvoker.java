package de.koelle.christian.actorshowcase.common;

import java.util.function.Predicate;

public class ConditionalRuntimeExceptionProvoker {

    private final Boolean produceIntentionalExceptionForTesting = Boolean.valueOf(System.getProperty(AppIds.SYS_PROP_PRODUCE_INTENTIONAL_EXCEPTION_FOR_TESTING, "false"));
    private final String intentionalExceptionTriggerZipFileName = System.getProperty(AppIds.SYS_PROP_INTENTIONAL_EXCEPTION_TRIGGER_ZIP_FILE_NAME);

        public void throwConditionalExceptionWhenEnabledForTesting(Predicate<String> fileNamePredicate) {
        if (produceIntentionalExceptionForTesting && fileNamePredicate.test(intentionalExceptionTriggerZipFileName)) {
            throw new IllegalStateException("Intentional exception on %s.".formatted(intentionalExceptionTriggerZipFileName));
        }
    }
}
