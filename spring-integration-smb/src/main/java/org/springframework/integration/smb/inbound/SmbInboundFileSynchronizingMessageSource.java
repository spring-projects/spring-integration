/**
 * Copyright 2002-2012 the original author or authors.
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
 * @since 2.1.1
 */
public class SmbInboundFileSynchronizingMessageSource extends AbstractInboundFileSynchronizingMessageSource<SmbFile> {

	// CHECKSTYLE:OFF
	private final static String componentType = "smb:inbound-channel-adapter";
	// CHECKSTYLE:ON
	private final String        toString;

	public SmbInboundFileSynchronizingMessageSource(AbstractInboundFileSynchronizer<SmbFile> _synchronizer) {
		this(_synchronizer, null);
	}

	public SmbInboundFileSynchronizingMessageSource(AbstractInboundFileSynchronizer<SmbFile> _synchronizer, Comparator<File> _comparator) {
		super(_synchronizer, _comparator);
		toString = getClass().getName() + "[componentType=" + componentType + ", synchronizer=" + _synchronizer + ", comparator=" + _comparator + "]";
	}

	@Override
	public String getComponentType() {
		return componentType;
	}

	@Override
	public String toString() {
		return toString;
	}

}
