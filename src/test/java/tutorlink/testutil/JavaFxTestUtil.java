package tutorlink.testutil;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;

/**
 * Runs UI integration checks on the shared JavaFX application thread.
 */
public class JavaFxTestUtil {
    /**
     * Starts or reuses JavaFX without shutting it down between test windows.
     */
    public static void initialize() throws Exception {
        FutureTask<Void> initialized = new FutureTask<>(() -> {
            Platform.setImplicitExit(false);
            return null;
        });
        try {
            Platform.startup(initialized);
        } catch (IllegalStateException e) {
            Platform.runLater(initialized);
        }
        initialized.get(10, TimeUnit.SECONDS);
    }

    /**
     * Runs an action on JavaFX and propagates its original failure to JUnit.
     */
    public static void runOnFxThread(TestAction action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            action.run();
            return null;
        });
        Platform.runLater(task);
        try {
            task.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error error) {
                throw error;
            }
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new AssertionError(cause);
        }
    }

    /**
     * An action that may execute commands or perform file checks.
     */
    @FunctionalInterface
    public interface TestAction {
        /** Runs the action. */
        void run() throws Exception;
    }
}
