/*
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.smb.inbound;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.codelibs.jcifs.smb.impl.SmbException;
import org.codelibs.jcifs.smb.impl.SmbFile;
import org.jspecify.annotations.Nullable;

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
 * @author Artem Bilan
 * @author Daniel Frey
 *
 * @since 6.0
 *
 */
public class SmbStreamingMessageSource extends AbstractRemoteFileStreamingMessageSource<SmbFile> {

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
	@SuppressWarnings("this-escape")
	public SmbStreamingMessageSource(RemoteFileTemplate<SmbFile> template, @Nullable Comparator<SmbFile> comparator) {
		super(template, comparator);
		doSetFilter(new SmbPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "smbStreamingMessageSource"));
	}

	@Override
	public String getComponentType() {
		return "smb:inbound-streaming-channel-adapter";
	}

	@Override
	protected List<AbstractFileInfo<SmbFile>> asFileInfoList(Collection<SmbFile> files) {
		List<AbstractFileInfo<SmbFile>> canonicalFiles = new ArrayList<>(files.size());
		for (SmbFile file : files) {
			canonicalFiles.add(new SmbFileInfo(file));
		}
		return canonicalFiles;
	}

	@Override
	protected boolean isDirectory(SmbFile file) {
		try {
			return file.isDirectory();
		}
		catch (SmbException se) {
			logger.error(se, "Unable to determine if this SmbFile represents a directory");
			return false;
		}
	}

}
