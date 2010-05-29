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
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * A {@link SocketWriter} that writes to a {@link java.net.Socket}. The 
 * data is wrapped in a wire protocol based on the messageFormat property.
 * 
 * @author Gary Russell
 * @since 2.0
 */
public class NetSocketWriter extends AbstractSocketWriter {

	protected Socket socket;
	
	protected ObjectOutputStream objectOutputStream;

	/**
	 * Constructs a NetSocketWriter for the Socket.
	 *
	 * @param socket The socket.
	 */
	public NetSocketWriter(Socket socket) {
		this.socket = socket;
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketWriter#writeCrLfFormat(byte[])
	 */
	@Override
	protected void writeCrLfFormat(byte[] bytes) throws IOException {
		OutputStream outputStream = this.socket.getOutputStream();
		outputStream.write(bytes);
		outputStream.write('\r');
		outputStream.write('\n');
		outputStream.flush();
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketWriter#writeCustomFormat(byte[])
	 */
	@Override
	protected void writeSerializedFormat(Object object) throws IOException {
		if (this.objectOutputStream == null) {
			OutputStream os = this.socket.getOutputStream();
			this.objectOutputStream = new ObjectOutputStream(os);
		}
		this.objectOutputStream.writeObject(object);
		this.objectOutputStream.flush();
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketWriter#writeCustomFormat(byte[])
	 */
	@Override
	protected void writeCustomFormat(Object object) throws IOException {
		throw new UnsupportedOperationException("Need to subclass for this format");
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketWriter#writeLengthFormat(byte[])
	 */
	@Override
	protected void writeLengthFormat(byte[] bytes) throws IOException {
		ByteBuffer lengthPart = ByteBuffer.allocate(4);
		lengthPart.putInt(bytes.length);
		OutputStream outputStream = this.socket.getOutputStream();
		outputStream.write(lengthPart.array());
		outputStream.write(bytes);
		outputStream.flush();
	}
	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketWriter#writeStxEtxFormat(byte[])
	 */
	@Override
	protected void writeStxEtxFormat(byte[] bytes) throws IOException {
		OutputStream outputStream = this.socket.getOutputStream();
		outputStream.write(STX);
		outputStream.write(bytes);
		outputStream.write(ETX);
		outputStream.flush();
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketWriter#doClose()
	 */
	@Override
	protected void doClose() {
		try {
			this.socket.close();
		} catch (IOException e) {
			logger.error("Error on close", e);
		}
	}
}
