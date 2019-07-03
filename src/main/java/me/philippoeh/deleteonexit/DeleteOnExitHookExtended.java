package me.philippoeh.deleteonexit;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;

/**
 * Serves as an replacement for the java.io.DeleteOnExitHook, because the original DeleteOnExitHook causes
 * memory leaks on long running applications.
 */
public class DeleteOnExitHookExtended {

    /**
     * Adds the file to the {@link DeleteOnExitHook}
     *
     * @param file file, which should be deleted on exit.
     */
    public static void deleteOnExit(File file) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkDelete(file.getPath());
        }
        if (file.getPath().indexOf('\u0000') < 0) {
            return;
        }
        DeleteOnExitHook.add(file.getPath());
    }

    /**
     * Removes the path of the file from the hook to free the memory of the file already was deleted.
     *
     * @param file file, which should be removed from the hook.
     */
    public static void removeFromOnExit(File file) {
        DeleteOnExitHook.remove(file.getPath());
    }

    /**
     * Taken from {@link java.io.DeleteOnExitHook} and extended to provide a method to remove added paths, which will
     * prevent memory leaks.
     */
    private static class DeleteOnExitHook {
        private static LinkedHashSet<String> files = new LinkedHashSet<String>();

        static {
            // DeleteOnExitHook must be the last shutdown hook to be invoked.
            // Application shutdown hooks may add the first file to the
            // delete on exit list and cause the DeleteOnExitHook to be
            // registered during shutdown in progress. So set the
            // registerShutdownInProgress parameter to true.
            sun.misc.SharedSecrets.getJavaLangAccess()
                    .registerShutdownHook(3 /* Shutdown hook invocation order */,
                            true /* register even if shutdown in progress */,
                            new Runnable() {
                                public void run() {
                                    runHooks();
                                }
                            }
                    );
        }

        private DeleteOnExitHook() {
        }

        static synchronized void add(String file) {
            if (files == null) {
                // DeleteOnExitHook is running. Too late to add a file
                throw new IllegalStateException("Shutdown in progress");
            }

            files.add(file);
        }

        static synchronized void remove(String file) {
            if (files != null) {
                // only if DeleteOnExitHook is not already running
                files.remove(file);
            }
        }

        static void runHooks() {
            LinkedHashSet<String> theFiles;

            synchronized (DeleteOnExitHook.class) {
                theFiles = files;
                files = null;
            }

            ArrayList<String> toBeDeleted = new ArrayList<String>(theFiles);

            // reverse the list to maintain previous jdk deletion order.
            // Last in first deleted.
            Collections.reverse(toBeDeleted);
            for (String filename : toBeDeleted) {
                (new File(filename)).delete();
            }
        }
    }
}
