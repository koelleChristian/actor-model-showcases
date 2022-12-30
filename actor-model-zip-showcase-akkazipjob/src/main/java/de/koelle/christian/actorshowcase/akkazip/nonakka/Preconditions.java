/*
 * Copyright 2015 IQTIG – Institut für Qualitätssicherung und Transparenz im Gesundheitswesen.
 * Dieser Code ist urheberrechtlich geschützt (Copyright). Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, beim IQTIG.
 * Wer gegen das Urheberrecht verstößt, macht sich gem. § 106 ff Urhebergesetz strafbar. Er wird zudem kostenpflichtig abgemahnt und muss
 * Schadensersatz leisten.
 */
package de.koelle.christian.actorshowcase.akkazip.nonakka;

import java.util.Collection;

;

/**
 * Bereinigte und teilweise erweiterte Implementierung der von Google Guava entlehnten gleichnamigen Klasse.
 * Eine Anleitung für die Nutzung findet sich im <a href="https://github.com/google/guava/wiki/PreconditionsExplained">Guava User Guide</a>.
 * Die privaten Methoden isBlank und length entstammen den StringUtils von Apache Commons Lang3.
 */
@SuppressWarnings({"unused", "UnusedReturnValue", "java:S1118"})
// java:S1118 -> Utility classes should not have public constructors
public final class Preconditions {

    public static boolean checkArgument(final boolean expression) {
        if (!expression) throw new IllegalArgumentException();
        return true;
    }

    public static boolean checkArgument(final boolean expression, final Object errorMessage) {
        if (!expression) throw new IllegalArgumentException(String.valueOf(errorMessage));
        return true;
    }

    public static boolean checkArgument(final boolean expression, final String errorMessageTemplate, final Object... errorMessageArgs) {
        if (!expression) throw new IllegalArgumentException(String.format(errorMessageTemplate, errorMessageArgs));
        return true;
    }

    public static <T> T checkArgumentNotNull(final T reference) {
        if (reference == null) throw new IllegalArgumentException();
        return reference;
    }

    /**
     * Eine CheckNotNull-Methode, die aus dem mitgegebenen Referenz-Namen gleich eine passende Nachricht generiert.
     * So muss nicht jedes mal eine neue Null-Exception-Nachricht geschrieben werden.
     * param referenceName ...
     * param reference ...
     * param <T> ...
     * return Die Referenz, falls nicht null
     * throws IllegalArgumentException falls die Referenz null ist
     */
    public static <T> T checkArgumentNotNull(final String referenceName, final T reference) {
        if (reference == null) {
            throw new IllegalArgumentException(String.format("%s darf nicht null sein", referenceName));
        }
        return reference;
    }

    public static <T> T checkArgumentNotNull(final T reference, final Object errorMessage) {
        if (reference == null) throw new IllegalArgumentException(String.valueOf(errorMessage));
        return reference;
    }

    public static <T> T checkArgumentNotNull(final T reference, final String errorMessageTemplate, final Object... errorMessageArgs) {
        if (reference == null)
            throw new IllegalArgumentException(String.format(errorMessageTemplate, errorMessageArgs));
        return reference;
    }

    public static String checkArgumentStringNotEmptyOrNull(final String text) {
        if (isBlank(text)) throw new IllegalArgumentException();
        return text;
    }

    public static String checkArgumentStringNotEmptyOrNull(final String text, final Object errorMessage) {
        if (isBlank(text)) throw new IllegalArgumentException(String.valueOf(errorMessage));
        return text;
    }

    public static String checkArgumentStringNotEmptyOrNull(final String text, final String errorMessageTemplate, final Object... errorMessageArgs) {
        if (isBlank(text)) throw new IllegalArgumentException(String.format(errorMessageTemplate, errorMessageArgs));
        return text;
    }

    public static <T> Collection<T> checkArgumentCollectionNotEmptyOrNull(Collection<T> collection) {
        if ((collection == null) || (collection.isEmpty())) throw new IllegalArgumentException();
        return collection;
    }

    public static <T> Collection<T> checkArgumentCollectionNotEmptyOrNull(Collection<T> collection, final Object errorMessage) {
        if ((collection == null) || (collection.isEmpty()))
            throw new IllegalArgumentException(String.valueOf(errorMessage));
        return collection;
    }

    public static <T> Collection<T> checkArgumentCollectionNotEmptyOrNull(Collection<T> collection, final String errorMessageTemplate, final Object... errorMessageArgs) {
        if ((collection == null) || (collection.isEmpty()))
            throw new IllegalArgumentException(String.format(errorMessageTemplate, errorMessageArgs));
        return collection;
    }

    public static boolean checkState(final boolean expression) {
        if (!expression) throw new IllegalStateException();
        return true;
    }

    public static boolean checkState(final boolean expression, final Object errorMessage) {
        if (!expression) throw new IllegalStateException(String.valueOf(errorMessage));
        return true;
    }

    public static boolean checkState(final boolean expression, final String errorMessageTemplate, final Object... errorMessageArgs) {
        if (!expression) throw new IllegalStateException(String.format(errorMessageTemplate, errorMessageArgs));
        return true;
    }

    public static <T> T checkNotNull(final T reference) {
        if (reference == null) throw new NullPointerException();
        return reference;
    }

    /**
     * Eine CheckNotNull-Methode, die aus dem mitgegebenen Referenz-Namen gleich eine passende Nachricht generiert.
     * So muss nicht jedes mal eine neue Null-Exception-Nachricht geschrieben werden.
     * AB JDK 14 kann stattdessen evtl. eine "helpful NPE" als Standard geworfen werden, da ab dann eine Nachricht mit Referenz Teil keder NPE ist
     * param referenceName ...
     * param reference ...
     * param <T> ...
     * return Die Referenz, falls nicht null
     * throws IllegalArgumentException falls die Referenz null ist
     */
    public static <T> T checkNotNull(final String referenceName, final T reference) {
        if (reference == null) {
            throw new NullPointerException(String.format("%s darf nicht null sein", referenceName));
        }
        return reference;
    }


    public static <T> T checkNotNull(final T reference, final Object errorMessage) {
        if (reference == null) throw new NullPointerException(String.valueOf(errorMessage));
        return reference;
    }

    public static <T> T checkNotNull(final T reference, final String errorMessageTemplate, final Object... errorMessageArgs) {
        if (reference == null) throw new NullPointerException(String.format(errorMessageTemplate, errorMessageArgs));
        return reference;
    }

    private static boolean isBlank(final CharSequence charSequence) {
        return charSequence == null || charSequence.chars().allMatch(Character::isWhitespace);
    }
}
