/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.ftp.inbound;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.AbstractRemoteFileStreamingMessageSource;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.ftp.filters.FtpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.ftp.session.FtpFileInfo;
import org.springframework.integration.metadata.SimpleMetadataStore;

/**
 * Message source for streaming FTP remote file contents.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class FtpStreamingMessageSource extends AbstractRemoteFileStreamingMessageSource<FTPFile> {

	/**
	 * Construct an instance with the supplied template.
	 * @param template the template.
	 */
	public FtpStreamingMessageSource(RemoteFileTemplate<FTPFile> template) {
		this(template, null);
	}

	/**
	 * Construct an instance with the supplied template and comparator.
	 * Note: the comparator is applied each time the remote directory is listed
	 * which only occurs when the previous list is exhausted.
	 * @param template the template.
	 * @param comparator the comparator.
	 */
	public FtpStreamingMessageSource(RemoteFileTemplate<FTPFile> template, Comparator<FTPFile> comparator) {
		super(template, comparator);
		doSetFilter(new FtpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "ftpStreamingMessageSource"));
	}

	@Override
	public String getComponentType() {
		return "ftp:inbound-streaming-channel-adapter";
	}

	@Override
	protected List<AbstractFileInfo<FTPFile>> asFileInfoList(Collection<FTPFile> files) {
		List<AbstractFileInfo<FTPFile>> canonicalFiles = new ArrayList<AbstractFileInfo<FTPFile>>();
		for (FTPFile file : files) {
			canonicalFiles.add(new FtpFileInfo(file));
		}
		return canonicalFiles;
	}

	@Override
	protected boolean isDirectory(FTPFile file) {
		return file != null && file.isDirectory();
	}

}
