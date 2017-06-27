package com.github.dilipkrish.zip;

import joptsimple.*;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.util.Arrays.asList;

public class CreateSelfExtractingZipCommand {

    private static final int CHUNK_SIZE = 1024;

    public void run(String... args) throws Exception {
        OptionParser parser = new OptionParser();
        OptionSpec<File> inputDirectoryOpt = parser.acceptsAll(
                asList("d", "directory"),
                "Directory to add to the zip archive")
                .withRequiredArg()
                .ofType(File.class)
                .required();
        OptionSpec<String> artifactNameOpt = parser.acceptsAll(
                asList("an", "artifactName"),
                "Name of the artifact to create (without the extension). " +
                        "Defaults to name of the name of the leaf input directory")
                .withOptionalArg()
                .ofType(String.class);
        OptionSpec<File> workingDirectoryOpt = parser.acceptsAll(
                asList("wd", "workingDirectory"),
                "Working directory. Zip file entries are created relative to this. Defaults to directory to be archived")
                .withOptionalArg()
                .ofType(File.class);

        try {
            OptionSet parsed = parser.parse(args);
            String workingDirectory = workingDirectory(
                    parsed,
                    inputDirectoryOpt,
                    workingDirectoryOpt);
            File inputDirectory = parsed.valueOf(inputDirectoryOpt);
            String artifactName = artifactName(parsed, artifactNameOpt, inputDirectory);

            if (!inputDirectory.exists()) {
                System.out.printf("Directory %s does not exist.%n", inputDirectory.getAbsolutePath());
                return;
            }
            String zipArchivePath = createZipArchive(artifactName, inputDirectory, workingDirectory);
            createSelfExtractingExecutable(artifactName, zipArchivePath);
        } catch (OptionException e) {
            parser.printHelpOn(System.out);
        }
    }

    private String artifactName(
            OptionSet parsed,
            OptionSpec<String> artifactNameOpt,
            File inputDirectory) {
        if (parsed.has(artifactNameOpt)) {
            return parsed.valueOf(artifactNameOpt);
        }
        String absolutePath = inputDirectory.getAbsolutePath();
        return absolutePath.substring(absolutePath.lastIndexOf(File.separator));
    }

    private void createSelfExtractingExecutable(
            String artifactName,
            String zipArchive) throws IOException {
        String fileName = String.format("%s.exe", artifactName);
        System.out.printf("Creating self extracting exe: %s%n", fileName);
        try(InputStream preamble = Application.class.getResourceAsStream("/7z.sfx")) {
            try (InputStream config = Application.class.getResourceAsStream("/config.txt")) {
                try (InputStream zip = Files.newInputStream(Paths.get(zipArchive))) {
                    try (FileOutputStream output = new FileOutputStream(fileName)) {
                        IOUtils.copy(preamble, output);
                        IOUtils.copy(config, output);
                        IOUtils.copy(zip, output);
                    }
                }
            }
        }
    }

    private String createZipArchive(
            String artifactName,
            File inputDirectory,
            String workingDirectory) throws IOException {

        String fileName = String.format("%s.7z", artifactName);
        System.out.printf("Creating 7z file: %s%n", fileName);
        try (SevenZOutputFile zipped = new SevenZOutputFile(new File(fileName))) {

            zipDir(
                    inputDirectory,
                    zipped,
                    workingDirectory);

            return fileName;
        }
    }

    /**
     * We want it to end with a {@link File#separator} so that zip entries can be relative to that. For e.g. if working directory is
     * /application/dilipkrish and the actual absolute path of a file to be archived is /application/dilipkrish/rf/file.txt.
     * We want the working  directory to be such that the entry relative to the working directory is just
     * rf/file.txt. We add the {@link File#separator} so that it is convenient to calculate the absolute path of the file to be archived
     * 
     * @param parsed set of parsed options
     * @param inputDirectoryOption spec of the input directory command line args
     * @param workingDirectoryOption spec of the working directory command line args
     * @return working directory that forces a {@link File#separator} suffix character.
     * @throws IOException
     */
    private String workingDirectory(
            OptionSet parsed,
            OptionSpec<File> inputDirectoryOption,
            OptionSpec<File> workingDirectoryOption) throws IOException {
        String workingDirectory;
        if (parsed.has(workingDirectoryOption)) {
            workingDirectory = parsed.valueOf(workingDirectoryOption).getCanonicalPath();
        } else {
            workingDirectory = parsed.valueOf(inputDirectoryOption).getCanonicalPath();
        }

        if (!workingDirectory.endsWith(File.separator)) {
            workingDirectory += File.separator;
        }
        System.out.printf("Using working directory: %s%n", workingDirectory);
        return workingDirectory;
    }


    /**
     * Recursively zips a directory
     * @param zipDir directory to add to archive
     * @param zipped zipped archive to add entries to
     * @param workingDirectory working directory relative to which the entries are created.
     * @throws IOException
     */
    private void zipDir(File zipDir, SevenZOutputFile zipped, String workingDirectory) throws IOException {
        System.out.printf("Zipping directory %s%n", zipDir.getAbsolutePath());

        //get a listing of the directory content
        String[] directories = zipDir.list();

        //loop through directories, and zip the files
        for (String each : directories) {
            File file = new File(zipDir, each);
            if (file.isDirectory()) {
                zipDir(file, zipped, workingDirectory);
            } else {
                addEntry(file, zipped, workingDirectory);
            }
        }
    }

    /**
     * Adds entries to the zipped archive
     * @param fileToAdd file to add to the archive
     * @param zipped zipped archive to add entries to
     * @param workingDirectory working directory relative to which the entries are created.
     * @throws IOException
     */
    private void addEntry(
            File fileToAdd,
            SevenZOutputFile zipped,
            String workingDirectory) throws IOException {

        String entryName = fileToAdd.getCanonicalPath().replace(workingDirectory, "");
        System.out.println(String.format("\tAdding entry %s", entryName));
        SevenZArchiveEntry entry = zipped.createArchiveEntry(
                fileToAdd,
                entryName);

        //create a new zip entry
        zipped.putArchiveEntry(entry);

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileToAdd))) {

            byte[] buffer = new byte[CHUNK_SIZE];
            int len;
            while ((len = in.read(buffer)) > 0) {
                zipped.write(buffer, 0, len);
            }

            zipped.closeArchiveEntry();
        }
    }
}
