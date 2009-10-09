/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.udp;

import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.InboundMessageMapper;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.OutboundMessageMapper;

/**
 * Message Mapper for converting to and from UDP DatagramPackets. When
 * converting to a Message, the payload will be a byte array containing the
 * data from the received packet. When converting from a Message, the payload
 * may be either a byte array or a String. The default charset for converting
 * a String to a byte array is UTF-8, but that may be changed by invoking the
 * {@link #setCharset(String)} method.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class DatagramPacketMessageMapper implements InboundMessageMapper<DatagramPacket>,
		OutboundMessageMapper<DatagramPacket> {

	private volatile String charset = "UTF-8";


	public void setCharset(String charset) {
		this.charset = charset;
	}

	public DatagramPacket fromMessage(Message<?> message) throws Exception {
		byte[] bytes = null;
		Object payload = message.getPayload();
		if (payload instanceof byte[]) {
			bytes = (byte[]) payload;
		}
		else if (payload instanceof String) {
			try {
				bytes = ((String) payload).getBytes(this.charset);
			}
			catch (UnsupportedEncodingException e) {
				throw new MessageHandlingException(message, e);
			}
		}
		else {
			throw new MessageHandlingException(message, "the datagram packet mapper expects " +
					"either a byte array or String payload, but received: " + payload.getClass());
		}
		return new DatagramPacket(bytes, bytes.length);
	}

	public Message<byte[]> toMessage(DatagramPacket packet) throws Exception {
		int offset = packet.getOffset();
		int length = packet.getLength();
		byte[] payload = new byte[length];
		System.arraycopy(packet.getData(), offset, payload, 0, length);
		Message<byte[]> message = null;
		if (payload.length > 0) {
			message = MessageBuilder.withPayload(payload)
					.setHeader(UdpHeaders.HOSTNAME, packet.getAddress().getHostName())
					.setHeader(UdpHeaders.IP_ADDRESS, packet.getAddress().getHostAddress())
					.build();
		}
		return message;
	}

}
