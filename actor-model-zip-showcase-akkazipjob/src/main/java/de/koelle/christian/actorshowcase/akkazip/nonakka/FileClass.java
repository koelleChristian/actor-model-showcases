package de.koelle.christian.actorshowcase.akkazip.nonakka;

public enum FileClass {
    PICTURES("jpg,JPG,jpeg,gif,GIG,png,PNG,BMP,bmp")
    /**/;

    public final String fileExtensions;

    FileClass(String fileExtensions) {
        this.fileExtensions = fileExtensions;
    }
}
