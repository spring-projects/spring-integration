package org.springframework.integration.file.monitors;

import java.io.File;


/**
 * Defines an interface for a component that reacts to file system events
 *
 * @author Josh Long
 */
public interface EventDrivenDirectoryMonitor {
    /**
     * the implementation should know how to publish events on the {@link FileAdditionListener}
     * for a given #directory
     *
     * @param directory         the directory to start watching from. Unspecified if this implies recursion or not.
     * @param fileAdditionListener the callback
     * @throws Exception if anything should go wrong
     */
    void monitor(File directory, FileAdditionListener fileAdditionListener) throws Exception;
}
