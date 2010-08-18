package org.springframework.integration.ftp;

import org.apache.commons.net.ftp.FTPFile;

import java.util.List;

/**
 * Filters out all the FTPFiles taken in a scan of the remote mount o
 *
 * @author Josh Long
 */
public interface FtpFileListFilter {
    List<FTPFile> filterFiles (FTPFile [] files);
}
