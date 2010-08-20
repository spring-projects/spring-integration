package org.springframework.integration.file.monitors;

import java.io.File;


/**
 * simply takes a hint and publishes an event as appropriate
 *
 * @author Josh Long
 */
public class DirectedEventDrivenFileMonitor extends AbstractEventDrivenFileMonitor {
    public void directlyNotifyOfNewFile(File file) {
        this.publishNewFileReceived(file);
    }
}
