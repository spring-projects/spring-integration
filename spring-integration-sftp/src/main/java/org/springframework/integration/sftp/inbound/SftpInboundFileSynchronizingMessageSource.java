/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.sftp.inbound;

import java.io.File;
import java.util.Comparator;

import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizingMessageSource;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 * A {@link org.springframework.integration.core.MessageSource} implementation for SFTP
 * that delegates to an InboundFileSynchronizer.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class SftpInboundFileSynchronizingMessageSource extends AbstractInboundFileSynchronizingMessageSource<ChannelSftp.LsEntry> {

	public SftpInboundFileSynchronizingMessageSource(AbstractInboundFileSynchronizer<LsEntry> synchronizer) {
		super(synchronizer);
	}

	public SftpInboundFileSynchronizingMessageSource(AbstractInboundFileSynchronizer<LsEntry> synchronizer, Comparator<File> comparator) {
		super(synchronizer, comparator);
	}


	public String getComponentType() {
		return "sftp:inbound-channel-adapter";
	}

}
