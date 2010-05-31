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

/**
 * Abstract SocketReader that handles data in 3 standard, and one custom
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
 * {@link #assembleDataCustomFormat()} which is invoked by {@link #assembleData()}
 * when the format is {@link MessageFormats#FORMAT_CUSTOM}. 
 * 
 * @author Gary Russell
 * @since 2.0
 *
 */
public abstract class AbstractSocketReader implements SocketReader, MessageFormats {

	protected int messageFormat = FORMAT_LENGTH_HEADER;

	/**
	 * The assembled data; must contain a reference when assembleData()
	 * returns true; will be set to null when getAssembledData() is called.
	 */
	protected Object assembledData;

	protected int maxMessageSize = 1024 * 60;
	
	/**
	 * Assembles data in format {@link #FORMAT_LENGTH_HEADER}.
	 * @return SocketReader.MESSAGE_COMPLETE when message is assembled, otherwise SocketReader.MESSAGE_IMCOMPLETE, or
	 * < 0 if socket closed before any data for a message is received.
	 * @throws IOException
	 */
	protected abstract int assembleDataLengthFormat() throws IOException;
	
	/**
	 * Assembles data in format {@link #FORMAT_STX_ETX}.
	 * @return SocketReader.MESSAGE_COMPLETE when message is assembled, otherwise SocketReader.MESSAGE_IMCOMPLETE, or
	 * < 0 if socket closed before any data for a message is received.
	 * @throws IOException
	 */
	protected abstract int assembleDataStxEtxFormat() throws IOException;
	
	/**
	 * Assembles data in format {@link #FORMAT_CRLF}.
	 * @return SocketReader.MESSAGE_COMPLETE when message is assembled, otherwise SocketReader.MESSAGE_IMCOMPLETE, or
	 * < 0 if socket closed before any data for a message is received.
	 * @throws IOException
	 */
	protected abstract int assembleDataCrLfFormat() throws IOException;

	/**
	 * Assembles data in format {@link #FORMAT_JAVA_SERIALIZED}
	 * @return SocketReader.MESSAGE_COMPLETE when message is assembled, otherwise SocketReader.MESSAGE_IMCOMPLETE, or
	 * < 0 if socket closed before any data for a message is received.
	 * @throws IOException
	 */
	protected abstract int assembleDataSerializedFormat() throws IOException;

	/**
	 * Assembles data in format {@link #FORMAT_CUSTOM}. Implementations must
	 * return false until the message is completely assembled, at which time
	 * the implementation must update assembledData to reference the assembled
	 * message.
	 * @return True when a message is completely assembled.
	 * @throws IOException
	 */
	protected abstract int assembleDataCustomFormat() throws IOException;
	
	public int assembleData() throws IOException {
		int result;
		try {
			switch (this.messageFormat) {
			case FORMAT_LENGTH_HEADER:
				result = assembleDataLengthFormat();
				break;
			case FORMAT_STX_ETX:
				result = assembleDataStxEtxFormat();
				break;
			case FORMAT_CRLF:
				result = assembleDataCrLfFormat();
				break;
			case FORMAT_JAVA_SERIALIZED:
				result = assembleDataSerializedFormat();
				break;
			case FORMAT_CUSTOM:
				result = assembleDataCustomFormat();
				break;
			default:
				throw new UnsupportedOperationException(
						"Unsupported message format: " + messageFormat);
			}
			if (result < 0) {
				doClose();
			}
			return result;
		} catch (IOException e) {
			doClose();
			throw e;
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.SocketReader#getAssembledData()
	 */
	public Object getAssembledData() {
		Object assembledData = this.assembledData;
		this.assembledData = null;
		if (assembledData instanceof byte[] &&
				((byte[]) assembledData).length == 0) {
			return null;
		}
		return assembledData;
	}

	/**
	 * Called after an exception; close the transport. 
	 */
	protected abstract void doClose();

	/**
	 * @param messageFormat the messageFormat to set,
	 */
	public void setMessageFormat(int messageFormat) {
		this.messageFormat = messageFormat;
	}

	public void setMaxMessageSize(int maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
	}
}
