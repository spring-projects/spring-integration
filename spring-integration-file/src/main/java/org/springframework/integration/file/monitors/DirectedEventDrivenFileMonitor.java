package org.springframework.integration.file.monitors;

import java.io.File;


/**
 * simply takes a cue / hint (something <emphasis>tells</emphasis> it outright that something has
 * been added to a directory, and it and publishes an event as appropriate). This is useful for adapters
 * that know when the file's been downloaded and want to deliver data as soon as its downloaded, but to poll the
 * remote system only at a certain interval.
 *
 * @author Josh Long
 */
public class DirectedEventDrivenFileMonitor extends AbstractEventDrivenFileMonitor {
    public void directlyNotifyOfNewFile(File file) {
        this.publishNewFileReceived(file);
    }
}
