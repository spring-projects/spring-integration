/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.smb.inbound;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.AbstractRemoteFileStreamingMessageSource;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.smb.filters.SmbPersistentAcceptOnceFileListFilter;
import org.springframework.integration.smb.session.SmbFileInfo;

/**
 * Message source for streaming SMB remote file contents.
 *
 * @author Gregory Bragg
 *
 * @since 6.0
 *
 */
public class SmbStreamingMessageSource extends AbstractRemoteFileStreamingMessageSource<SmbFile> {

	private static final Log logger = LogFactory.getLog(SmbStreamingMessageSource.class);

	/**
	 * Construct an instance with the supplied template.
	 * @param template the template.
	 */
	public SmbStreamingMessageSource(RemoteFileTemplate<SmbFile> template) {
		this(template, null);
	}

	/**
	 * Construct an instance with the supplied template and comparator.
	 * Note: the comparator is applied each time the remote directory is listed
	 * which only occurs when the previous list is exhausted.
	 * @param template the template.
	 * @param comparator the comparator.
	 */
	public SmbStreamingMessageSource(RemoteFileTemplate<SmbFile> template, Comparator<SmbFile> comparator) {
		super(template, comparator);
		doSetFilter(new SmbPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "smbStreamingMessageSource"));
	}

	@Override
	public String getComponentType() {
		return "smb:inbound-streaming-channel-adapter";
	}

	@Override
	protected List<AbstractFileInfo<SmbFile>> asFileInfoList(Collection<SmbFile> files) {
		List<AbstractFileInfo<SmbFile>> canonicalFiles = new ArrayList<>();
		for (SmbFile file : files) {
			canonicalFiles.add(new SmbFileInfo(file));
		}
		return canonicalFiles;
	}

	@Override
	protected boolean isDirectory(SmbFile file) {
		try {
			return file != null && file.isDirectory();
		}
		catch (SmbException se) {
			logger.error("Unable to determine if this SmbFile represents a directory", se);
			return false;
		}
	}

}
