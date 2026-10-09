/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ftp.inbound;

import java.io.File;
import java.util.Comparator;

import org.apache.commons.net.ftp.FTPFile;
import org.jspecify.annotations.Nullable;

import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizingMessageSource;

/**
 * A {@link org.springframework.integration.core.MessageSource} implementation for FTP.
 *
 * @author Iwein Fuld
 * @author Josh Long
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class FtpInboundFileSynchronizingMessageSource extends AbstractInboundFileSynchronizingMessageSource<FTPFile> {

	public FtpInboundFileSynchronizingMessageSource(AbstractInboundFileSynchronizer<FTPFile> synchronizer) {
		super(synchronizer);
	}

	public FtpInboundFileSynchronizingMessageSource(AbstractInboundFileSynchronizer<FTPFile> synchronizer,
			@Nullable Comparator<File> comparator) {

		super(synchronizer, comparator);
	}

	public String getComponentType() {
		return "ftp:inbound-channel-adapter";
	}

}
