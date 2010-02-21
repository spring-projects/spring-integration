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
 *
 */
public abstract class AbstractSocketReader implements SocketReader, MessageFormats {

	protected int messageFormat = FORMAT_LENGTH_HEADER;

	/**
	 * The assembled data; must contain a reference when assembleData()
	 * returns true; will be set to null when getAssembledData() is called.
	 */
	protected byte[] assembledData;
	
	/**
	 * Assembles data in format {@link #FORMAT_LENGTH_HEADER}.
	 * @return True when a message is completely assembled.
	 * @throws IOException
	 */
	protected abstract boolean assembleDataLengthFormat() throws IOException;
	
	/**
	 * Assembles data in format {@link #FORMAT_STX_ETX}.
	 * @return True when a message is completely assembled.
	 * @throws IOException
	 */
	protected abstract boolean assembleDataStxEtxFormat() throws IOException;
	
	/**
	 * Assembles data in format {@link #FORMAT_CRLF}.
	 * @return True when a message is completely assembled.
	 * @throws IOException
	 */
	protected abstract boolean assembleDataCrLfFormat() throws IOException;
	
	/**
	 * Assembles data in format {@link #FORMAT_CUSTOM}. Implementations must
	 * return false until the message is completely assembled, at which time
	 * the implementation must update assembledData to reference the assembled
	 * message.
	 * @return True when a message is completely assembled.
	 * @throws IOException
	 */
	protected abstract boolean assembleDataCustomFormat() throws IOException;
	
	public boolean assembleData() throws IOException {
		switch (this.messageFormat) {
		case FORMAT_LENGTH_HEADER:
			return assembleDataLengthFormat();
		case FORMAT_STX_ETX:
			return assembleDataStxEtxFormat();
		case FORMAT_CRLF:
			return assembleDataCrLfFormat();
		case FORMAT_CUSTOM:
			return assembleDataCustomFormat();
		default:
			throw new UnsupportedOperationException(
					"Unsupported message format: " + messageFormat);
		}
	}

	/**
	 * @param messageFormat the messageFormat to set,
	 */
	public void setMessageFormat(int messageFormat) {
		this.messageFormat = messageFormat;
	}

}
