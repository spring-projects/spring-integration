package org.springframework.integration.sftp;

import com.jcraft.jsch.ChannelSftp;
import org.springframework.integration.file.entries.EntryNamer;

/**
 * Knows how to name a {@link com.jcraft.jsch.ChannelSftp.LsEntry} instance
 *
 * @author Josh Long
 */
public class SftpEntryNamer implements EntryNamer<ChannelSftp.LsEntry>{

    public String nameOf(ChannelSftp.LsEntry entry) {
     return entry.getFilename() ;
    }
}
