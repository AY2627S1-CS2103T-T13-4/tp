package tutorlink.commons.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Writes and reads files
 */
public class FileUtil {

    /**
     * Creates parent directories of file if it has a parent directory
     */
    private static void createParentDirsOfFile(Path file) throws IOException {
        Path parentDir = file.getParent();

        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }
    }

    /**
     * Assumes file exists
     */
    public static String readFromFile(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    /**
     * Writes given string to a file.
     * Will create the file if it does not exist yet.
     */
    public static void writeToFile(Path file, String content) throws IOException {
        writeToFile(file, content, (temporaryFile, bytes) -> Files.write(temporaryFile, bytes));
    }

    /**
     * Writes through a temporary file so a failed write cannot truncate the previous target.
     * The writer parameter lets tests reproduce a partial temporary-file write reliably.
     */
    static void writeToFile(Path file, String content, TemporaryFileWriter writer) throws IOException {
        Path absoluteFile = file.toAbsolutePath();
        if (Files.exists(absoluteFile) && !Files.isWritable(absoluteFile)) {
            throw new AccessDeniedException(absoluteFile.toString());
        }
        createParentDirsOfFile(absoluteFile);
        Path temporaryFile = Files.createTempFile(absoluteFile.getParent(), ".tutorlink-", ".tmp");

        try {
            writer.write(temporaryFile, content.getBytes(StandardCharsets.UTF_8));
            replaceFile(temporaryFile, absoluteFile);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static void replaceFile(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @FunctionalInterface
    interface TemporaryFileWriter {
        void write(Path file, byte[] content) throws IOException;
    }

}
