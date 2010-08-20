/*
 * Copyright 2010 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.sftp;

import com.jcraft.jsch.ChannelSftp;
import org.springframework.util.Assert;

import java.util.*;


/**
 * Patterned very much on the {@link org.springframework.integration.file.filters.CompositeFileListFilter}
 *
 * @author Josh Long
 */       @Deprecated
public class CompositeFtpFileListFilter implements SftpFileListFilter {
    private Set<SftpFileListFilter> filters;

    public CompositeFtpFileListFilter(SftpFileListFilter... ftpFileListFilter) {
        this.filters = new LinkedHashSet<SftpFileListFilter>(Arrays.asList(ftpFileListFilter));
    }

    public CompositeFtpFileListFilter(Collection<SftpFileListFilter> ftpFileListFilter) {
        this.filters = new LinkedHashSet<SftpFileListFilter>(ftpFileListFilter);
    }

    public void addFilter(SftpFileListFilter ftpFileListFilter) {
        this.filters.add(ftpFileListFilter);
    }

    public List<ChannelSftp.LsEntry> filterFiles(ChannelSftp.LsEntry[] files) {
        Assert.notNull(files, "files[] can't be null!");

        List<ChannelSftp.LsEntry> leftOver = Arrays.asList(files);

        for (SftpFileListFilter ff : this.filters)
            leftOver = ff.filterFiles(leftOver.toArray(new ChannelSftp.LsEntry[leftOver.size()]));

        return leftOver;
    }
}
