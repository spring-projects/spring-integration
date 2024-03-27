/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.ftp.inbound;

import java.io.File;
import java.util.Comparator;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizingMessageSource;

/**
 * A {@link org.springframework.integration.core.MessageSource} implementation for FTP.
 *
 * @author Iwein Fuld
 * @author Josh Long
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class FtpInboundFileSynchronizingMessageSource extends AbstractInboundFileSynchronizingMessageSource<FTPFile> {

	public FtpInboundFileSynchronizingMessageSource(AbstractInboundFileSynchronizer<FTPFile> synchronizer) {
		super(synchronizer);
	}

	public FtpInboundFileSynchronizingMessageSource(AbstractInboundFileSynchronizer<FTPFile> synchronizer, Comparator<File> comparator) {
		super(synchronizer, comparator);
	}

	public String getComponentType() {
		return "ftp:inbound-channel-adapter";
	}

}
