/*
 * Copyright © 2012 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.integration.smb.inbound;

import java.io.File;
import java.util.Comparator;

import jcifs.smb.SmbFile;

import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizingMessageSource;

/**
 * A {@link org.springframework.integration.core.MessageSource} implementation for SMB.
 *
 * @author Markus Spann
 *
 * @since 6.0
 */
public class SmbInboundFileSynchronizingMessageSource extends AbstractInboundFileSynchronizingMessageSource<SmbFile> {

	public SmbInboundFileSynchronizingMessageSource(AbstractInboundFileSynchronizer<SmbFile> _synchronizer) {
		this(_synchronizer, null);
	}

	public SmbInboundFileSynchronizingMessageSource(AbstractInboundFileSynchronizer<SmbFile> _synchronizer,
			Comparator<File> _comparator) {
		super(_synchronizer, _comparator);
	}

	@Override
	public String getComponentType() {
		return "smb:inbound-channel-adapter";
	}

}
