package org.springframework.integration.ftp;

import org.apache.commons.net.ftp.FTPFile;
import org.springframework.util.Assert;

import java.util.*;


/**
 * Patterned very much on the {@link org.springframework.integration.file.CompositeFileListFilter}
 *
 * @author Josh Long
 */
public class CompositeFTPFileListFilter implements FTPFileListFilter {
    private Set<FTPFileListFilter> filters;

    public CompositeFTPFileListFilter(FTPFileListFilter... ftpFileListFilter) {
        this.filters = new LinkedHashSet<FTPFileListFilter>(Arrays.asList(ftpFileListFilter));
    }

    public CompositeFTPFileListFilter(Collection<FTPFileListFilter> ftpFileListFilter) {
        this.filters = new LinkedHashSet<FTPFileListFilter>(ftpFileListFilter);
    }
       public void addFilter( FTPFileListFilter ftpFileListFilter ) {
           this.filters.add(ftpFileListFilter);
       }
    public List<FTPFile> filterFiles(FTPFile[] files) {
        Assert.notNull(files, "files[] can't be null!");

        List<FTPFile> leftOver = Arrays.asList(files);

        for (FTPFileListFilter ff : this.filters)
            leftOver = ff.filterFiles(leftOver.toArray(new FTPFile[leftOver.size()]));

        return leftOver;
    }
}
