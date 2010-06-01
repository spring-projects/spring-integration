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
package org.springframework.integration.ip.tcp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Reads messages that are exactly 24 bytes long.
 * 
 * @author Gary Russell
 *
 */
public class CustomNioSocketReader extends NioSocketReader {

	private ByteBuffer buffer;

	public CustomNioSocketReader() {
		super(null);
	}
	
	public CustomNioSocketReader(SocketChannel channel) {
		super(channel);
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.NetSocketReader#assembleDataCustomFormat()
	 */
	@Override
	protected int assembleDataCustomFormat() throws IOException {
		if (buffer == null) {
			buffer = allocate(24);
		}
		int status = readChannel(buffer);
		if (status < 0 ) {
			if (buffer.remaining() == 24)
				return status;
			throw new IOException("Channel closed");
		}
		if (buffer.hasRemaining()) {
			return MESSAGE_INCOMPLETE;
		}
		assembledData = buffer.array();
		buffer = null;
		return MESSAGE_COMPLETE;
	}
	
	

}
