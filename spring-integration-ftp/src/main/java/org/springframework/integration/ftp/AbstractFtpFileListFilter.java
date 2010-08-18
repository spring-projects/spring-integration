package org.springframework.integration.ftp;

import org.apache.commons.net.ftp.FTPFile;

import java.util.ArrayList;
import java.util.List;


/**
 * Convenience implementation patterned off {@link org.springframework.integration.file.FileListFilter}
 *
 * @author Josh Long
 */
public abstract class AbstractFtpFileListFilter implements FtpFileListFilter {
    /**
     * {@inheritDoc}
     */
    abstract public boolean accept(FTPFile ftpFile);

    public List<FTPFile> filterFiles(FTPFile[] files) {
        List<FTPFile> accepted = new ArrayList<FTPFile>();

        if (files != null) {
            for (FTPFile f : files) {
                if (this.accept(f)) {
                    accepted.add(f);
                }
            }
        }

        return accepted;
    }
}
