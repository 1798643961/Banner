package com.mohistmc.banner.library;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

/**
 * Simple library manager which downloads external dependencies.
 */
public final class LibraryManager {

    private final Logger LOGGER = LogManager.getLogger("Mohist Banner");

    /**
     * The Maven repository to download from.
     */
    private final String defaultRepository;

    /**
     * The directory to store downloads in.
     */
    private final File directory;

    /**
     * Whether the checksum of each library should be verified after being downloaded.
     */
    private final boolean validateChecksum;

    /**
     * The maximum amount of attempts to download each library.
     */
    private final int maxDownloadAttempts;

    private final Collection<Library> libraries;

    /**
     * Creates the instance.
     */
    public LibraryManager(String defaultRepository, String directoryName, boolean validateChecksum, int maxDownloadAttempts, Collection<Library> libraries) {
        checkNotNull(defaultRepository);
        checkNotNull(directoryName);
        this.defaultRepository = defaultRepository;
        this.directory = new File(directoryName);
        this.validateChecksum = validateChecksum;
        this.maxDownloadAttempts = maxDownloadAttempts;
        this.libraries = libraries;
    }

    /**
     * Downloads the libraries.
     */
    public void run() {
        if (!directory.isDirectory() && !directory.mkdirs())
            LOGGER.error("Could not create libraries directory: " + directory);

        for (Library lib : libraries) {
            String fn = lib.libraryKey.artifactId + "-" + lib.version + ".jar";
            File f = new File(directory, fn);
            if (f.isFile() && !(fn.contains("intermediary-adapter")) ) {
                try {
                    KnotHelper.propose(f);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            } else {
                download(lib);
            }
        }
    }

    public void download(Library library) {
        String repository = library.repository;
        if (repository == null) repository = defaultRepository;
        if (!repository.endsWith("/")) repository += "/";

        String fileName = library.libraryKey.artifactId + '-' + library.version + ".jar";

        File file = new File(directory, fileName);
        if (!file.exists()) {
            int attempts = 0;
            while (attempts < maxDownloadAttempts) {
                attempts++;
                // download it
                LOGGER.info("Downloading " + library.toString() + "...");
                try {
                    URL downloadUrl;
                    downloadUrl = new URL(repository + library.libraryKey.groupId.replace('.', '/') + '/' + library.libraryKey.artifactId + '/' + library.version
                                + '/' + library.libraryKey.artifactId + '-' + library.version + ".jar");
                    URLConnection connection = getConn(downloadUrl.toString());
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 Chrome/90.0.4430.212");

                    try (ReadableByteChannel input = Channels.newChannel(connection.getInputStream()); FileOutputStream output = new FileOutputStream(file)) {
                        output.getChannel().transferFrom(input, 0, Long.MAX_VALUE);
                        LOGGER.info("Downloaded " + library.toString() + '.');
                    }

                    if (validateChecksum && library.checksumType != null && library.checksumValue != null && !checksum(file, library)) {
                        LOGGER.error("The checksum for the library '" + fileName + "' does not match. " + (attempts == maxDownloadAttempts ?
                                "Restart the server to attempt downloading it again." : "Attempting download again ("+ (attempts+1) +"/"+ maxDownloadAttempts +")"));
                        file.delete();
                        if (attempts == maxDownloadAttempts) return;
                        continue;
                    }
                    // everything's fine
                    break;
                } catch (IOException e) {
                    LOGGER.warn( "Failed to download: " + library.toString(), e);
                    file.delete();
                    if (attempts == maxDownloadAttempts) {
                        LOGGER.warn("Restart the server to attempt downloading '" + fileName + "' again.");
                        return;
                    }
                    LOGGER.warn("Attempting download of '" + fileName + "' again (" + (attempts + 1) + "/" + maxDownloadAttempts + ")");
                }
            }
        } else if (validateChecksum && library.checksumType != null && library.checksumValue != null && !checksum(file, library)) {
            // The file is already downloaded, but validate the checksum as a warning only
            LOGGER.warn("The checksum for the library '" + fileName + "' does not match. Remove the library and restart the server to download it again.");
        }

        addToKnot(library, file);
    }

    void addToKnot(Library library, File file) {
        // Add to KnotClassLoader
        try {
            if (!library.libraryKey.artifactId.contains("adapter")) {
                KnotHelper.propose(file);
            }
        } catch (Exception e) {
            LOGGER.warn( "Failed to add to classpath: " + library.toString(), e);
        }
    }

    public static URLConnection getConn(String URL) {
        URLConnection conn = null;
        try {
            conn = new URL(URL).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return conn;
    }

    boolean checksum(File file, Library library) {
        checkNotNull(file);
        if (!file.exists()) return false;

        HashAlgorithm algorithm = library.checksumType;
        String checksum = library.checksumValue;
        if (algorithm == null || checksum == null || checksum.isEmpty()) return true; // assume everything is OK if no reference checksum is provided

        // get the file digest
        String digest;
        try {
            digest = Files.hash(file, algorithm.function).toString();
        } catch (IOException ex) {
            LOGGER.error("Failed to compute digest for '" + file.getName() + "'", ex);
            return false;
        }
        //System.out.println(file.getName() + ": " + digest);
        return digest.equals(checksum);
    }

    /**
     * An enum containing the supported hash algorithms.
     */
    public enum HashAlgorithm {
        SHA1(Hashing.sha1()), // The SHA-1 hash algorithm.
        MD5(Hashing.md5()); // The MD5 hash algorithm.

        public final HashFunction function;

        private HashAlgorithm(HashFunction function) {
            this.function = function;
        }
    }

}