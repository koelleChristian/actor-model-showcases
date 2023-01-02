package de.koelle.christian.actorshowcase.common;

public enum FileClass {
    IMAGES("jpg,JPG,jpeg,gif,GIG,png,PNG,BMP,bmp"),
    TXT("txt,TXT"),
    /**/;

    public final String fileExtensions;

    FileClass(String fileExtensions) {
        this.fileExtensions = fileExtensions;
    }
}
