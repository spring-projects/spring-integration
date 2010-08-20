package org.springframework.integration.file.monitors;

import java.io.File;

/**
 * A generic hook into the arrival of a new {@link java.io.File}
 *
 * @author Josh Long
 * @see org.springframework.integration.file.monitors.MessageSendingFileAdditionListener
 */
public interface FileAdditionListener {

    /**
     * a callback method that's invoked when a new {@link java.io.File} is detected.
     *
     * @param f the {@link java.io.File} that was detected
     */
    void fileAdded(File f);
}
