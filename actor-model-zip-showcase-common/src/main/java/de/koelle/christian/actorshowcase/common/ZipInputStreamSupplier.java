package de.koelle.christian.actorshowcase.common;

import java.io.InputStream;

@FunctionalInterface
public interface ZipInputStreamSupplier {

    InputStream get();
}