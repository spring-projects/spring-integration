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
import java.net.Socket;

/**
 * Writes packets that are always 24 bytes long.
 * @author Gary Russell
 *
 */
public class CustomNetSocketWriter extends NetSocketWriter {

	/**
	 * @param socket
	 */
	public CustomNetSocketWriter(Socket socket) {
		super(socket);
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.NetSocketWriter#writeCustomFormat(byte[])
	 */
	@Override
	protected void writeCustomFormat(Object object) throws IOException {
		byte[] bytes;
		if (object instanceof byte[]) {
			bytes = (byte[]) object;
		} else if (object instanceof String) {
			bytes = ((String) object).getBytes();
		} else {
			throw new UnsupportedOperationException("Only supports String and byte[]");
		}
		
		if (bytes.length > 24) {
			socket.getOutputStream().write(bytes, 0, 24);
			return;
		}
		socket.getOutputStream().write(bytes);
		if (bytes.length < 24) {
			socket.getOutputStream().write(
				"                        ".substring(bytes.length) .getBytes());
		}
	}
	
	

}
