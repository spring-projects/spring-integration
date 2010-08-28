package org.springframework.integration.ftp;

import org.apache.commons.net.ftp.FTPFile;
import org.springframework.integration.file.entries.EntryNamer;


/**
 * A {@link org.springframework.integration.file.entries.EntryNamer} for {@link org.apache.commons.net.ftp.FTPFile} objects
 *
 * @author Josh Long
 */
public class FtpFileEntryNamer implements EntryNamer<FTPFile> {
	public String nameOf(FTPFile entry) {
		return entry.getName();
	}
}
