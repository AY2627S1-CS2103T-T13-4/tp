package tutorlink.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileUtilTest {
    @TempDir
    Path temporaryFolder;

    @Test
    void writeToFile_missingParentDirectories_createsFile() throws IOException {
        Path target = temporaryFolder.resolve("missing").resolve("directory").resolve("data.json");

        FileUtil.writeToFile(target, "new roster");

        assertEquals("new roster", Files.readString(target));
    }

    @Test
    void writeToFile_existingFile_replacesContent() throws IOException {
        Path target = temporaryFolder.resolve("data.json");
        Files.writeString(target, "old roster");

        FileUtil.writeToFile(target, "new roster");

        assertEquals("new roster", Files.readString(target));
    }

    @Test
    void writeToFile_partialTemporaryWriteFails_preservesTargetAndCleansTemporaryFile() throws IOException {
        Path target = temporaryFolder.resolve("data.json");
        Files.writeString(target, "old roster");

        assertThrows(IOException.class, () -> FileUtil.writeToFile(target, "new roster", (temporaryFile, bytes) -> {
            Files.writeString(temporaryFile, "partial");
            throw new IOException("Simulated interrupted write");
        }));

        assertEquals("old roster", Files.readString(target));
        try (Stream<Path> files = Files.list(temporaryFolder)) {
            assertEquals(List.of(target), files.toList());
        }
    }

    @Test
    void writeToFile_readOnlyTarget_throwsAccessDeniedExceptionAndPreservesTarget() throws IOException {
        Path target = temporaryFolder.resolve("data.json");
        Files.writeString(target, "old roster");
        Assumptions.assumeTrue(Files.getFileStore(target).supportsFileAttributeView("posix"));
        Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("r--r--r--"));

        try {
            assertThrows(AccessDeniedException.class, () -> FileUtil.writeToFile(target, "new roster"));
            assertEquals("old roster", Files.readString(target));
        } finally {
            Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rw-------"));
        }
    }
}
