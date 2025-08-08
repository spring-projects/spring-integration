/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.sftp.inbound;

import java.io.File;
import java.util.Comparator;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizingMessageSource;

/**
 * A {@link org.springframework.integration.core.MessageSource} implementation for SFTP
 * that delegates to an InboundFileSynchronizer.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class SftpInboundFileSynchronizingMessageSource
		extends AbstractInboundFileSynchronizingMessageSource<SftpClient.DirEntry> {

	public SftpInboundFileSynchronizingMessageSource(AbstractInboundFileSynchronizer<SftpClient.DirEntry> synchronizer) {
		super(synchronizer);
	}

	public SftpInboundFileSynchronizingMessageSource(AbstractInboundFileSynchronizer<SftpClient.DirEntry> synchronizer,
			Comparator<File> comparator) {

		super(synchronizer, comparator);
	}

	public String getComponentType() {
		return "sftp:inbound-channel-adapter";
	}

}
