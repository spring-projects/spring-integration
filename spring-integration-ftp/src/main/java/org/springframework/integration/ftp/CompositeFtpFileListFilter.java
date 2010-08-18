package org.springframework.integration.ftp;

import org.apache.commons.net.ftp.FTPFile;
import org.springframework.util.Assert;

import java.util.*;


/**
 * Patterned very much on the {@link org.springframework.integration.file.CompositeFileListFilter}
 *
 * @author Josh Long
 */
public class CompositeFtpFileListFilter implements FtpFileListFilter {
    private Set<FtpFileListFilter> filters;

    public CompositeFtpFileListFilter(FtpFileListFilter... ftpFileListFilter) {
        this.filters = new LinkedHashSet<FtpFileListFilter>(Arrays.asList(ftpFileListFilter));
    }

    public CompositeFtpFileListFilter(Collection<FtpFileListFilter> ftpFileListFilter) {
        this.filters = new LinkedHashSet<FtpFileListFilter>(ftpFileListFilter);
    }
       public void addFilter( FtpFileListFilter ftpFileListFilter ) {
           this.filters.add(ftpFileListFilter);
       }
    public List<FTPFile> filterFiles(FTPFile[] files) {
        Assert.notNull(files, "files[] can't be null!");

        List<FTPFile> leftOver = Arrays.asList(files);

        for (FtpFileListFilter ff : this.filters)
            leftOver = ff.filterFiles(leftOver.toArray(new FTPFile[leftOver.size()]));

        return leftOver;
    }
}
