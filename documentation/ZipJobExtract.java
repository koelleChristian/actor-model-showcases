public record ZipJob(
        Optional<Path> targetFolderPath,
        String targetZipFileName,
        Map<String, ZipInputStreamSupplier> filesToBeIncluded,
        Set<ZipJob> subZipJobs) {
}