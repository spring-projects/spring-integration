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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract SocketWriter that handles data in 3 standard, and one custom
 * format. The default format is {@link MessageFormats#FORMAT_LENGTH_HEADER} in which 
 * the message consists of a 4 byte integer (in network byte order) containing
 * the length of data that follows. {@link MessageFormats#FORMAT_STX_ETX} 
 * indicates a message where the data begins with STX (0x02) and ends with 
 * ETX (0x03); the STX and ETX are not part of the data. {@link MessageFormats#FORMAT_CRLF}
 * indicates a message followed by carriage return and line feed '\r\n'. 
 * FORMAT_LENGTH_HEADER can be used for {@link java.net.Socket} and 
 * {@link java.nio.channels.SocketChannel} implementations are provided for
 * the standard formats. Users requiring other formats should subclass the
 * appropriate implementation, and provide an implementation for 
 * {@link #writeCustomFormat(byte[])} which is invoked by {@link #write(byte[])}
 * when the format is {@link MessageFormats#FORMAT_CUSTOM}. 
 * 
 * @author Gary Russell
 * @since 2.0
 *
 */
public abstract class AbstractSocketWriter implements SocketWriter, MessageFormats {

	protected int messageFormat = FORMAT_LENGTH_HEADER;

    protected final Log logger = LogFactory.getLog(this.getClass());

    /*
	 * @see org.springframework.integration.ip.tcp.SocketWriter#write(byte[])
	 */
	public synchronized void write(byte[] bytes) throws IOException {
		try {
			switch (this.messageFormat) {
			case FORMAT_LENGTH_HEADER:
				writeLengthFormat(bytes);
				return;
			case FORMAT_STX_ETX:
				writeStxEtxFormat(bytes);
				return;
			case FORMAT_CRLF:
				writeCrLfFormat(bytes);
				return;
			case FORMAT_CUSTOM:
				writeCustomFormat(bytes);
				return;
			default:
				throw new UnsupportedOperationException(
						"Unsupported message format: " + messageFormat);
			}
		}
		catch (IOException e) {
			doClose();
			throw e;
		}

	}

	/**
	 * Called when an IO error
	 */
	protected abstract void doClose();
	

	/**
	 * Write the length of the data in a 4 byte integer (in network byte
	 * order) before the data itself.
	 * @param bytes The bytes to write.
	 * @throws IOException 
	 */
	protected abstract void writeLengthFormat(byte[] bytes) throws IOException;

	/**
	 * Write an STX (0x02) followed by the data, followed by ETX (0x03). 
	 * @param bytes The bytes to write.
	 */
	protected abstract void writeStxEtxFormat(byte[] bytes) throws IOException;

	/**
	 * Write the data, followed by carriage return, line feed ('\r\n').
	 * @param bytes
	 */
	protected abstract void writeCrLfFormat(byte[] bytes) throws IOException;

	/**
	 * Write the data using some custom protocol.
	 * @param bytes
	 */
	protected abstract void writeCustomFormat(byte[] bytes) throws IOException;

	/**
	 * @param messageFormat the messageFormat to set
	 */
	public void setMessageFormat(int messageFormat) {
		this.messageFormat = messageFormat;
	}

}