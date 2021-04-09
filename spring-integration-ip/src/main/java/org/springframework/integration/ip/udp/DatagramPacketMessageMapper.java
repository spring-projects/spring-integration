/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.ip.udp;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.util.RegexUtils;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.mapping.MessageMappingException;
import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Message Mapper for converting to and from UDP DatagramPackets. When
 * converting to a Message, the payload will be a byte array containing the
 * data from the received packet. When converting from a Message, the payload
 * may be either a byte array or a String. The default charset for converting
 * a String to a byte array is UTF-8, but that may be changed by invoking the
 * {@link #setCharset(String)} method.
 *
 * By default, the UDP messages will be unreliable (truncation may occur on
 * the receiving end; packets may be lost).
 *
 * Reliability can be enhanced by one or both of the following techniques:
 * <ul>
 *   <li>including a binary message length at the beginning of the packet</li>
 *   <li>requesting a receipt acknowledgment</li>
 * </ul>
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Dave Syer
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class DatagramPacketMessageMapper implements InboundMessageMapper<DatagramPacket>,
		OutboundMessageMapper<DatagramPacket>, BeanFactoryAware {

	private volatile String charset = "UTF-8";

	private boolean acknowledge = false;

	private String ackAddress;

	private boolean lengthCheck = false;

	private boolean lookupHost = true;

	private volatile MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	private volatile boolean messageBuilderFactorySet;

	private static Pattern udpHeadersPattern =
			Pattern.compile(RegexUtils.escapeRegexSpecials(IpHeaders.ACK_ADDRESS) +
					"=" + "([^;]*);" +
					RegexUtils.escapeRegexSpecials(MessageHeaders.ID) +
					"=" + "([^;]*);");

	private BeanFactory beanFactory;

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public void setAcknowledge(boolean acknowledge) {
		this.acknowledge = acknowledge;
	}

	public void setAckAddress(String ackAddress) {
		this.ackAddress = ackAddress;
	}

	public void setLengthCheck(boolean lengthCheck) {
		this.lengthCheck = lengthCheck;
	}

	/**
	 * @param lookupHost the lookupHost to set
	 */
	public void setLookupHost(boolean lookupHost) {
		this.lookupHost = lookupHost;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	protected MessageBuilderFactory getMessageBuilderFactory() {
		if (!this.messageBuilderFactorySet) {
			if (this.beanFactory != null) {
				this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.beanFactory);
			}
			this.messageBuilderFactorySet = true;
		}
		return this.messageBuilderFactory;
	}

	/**
	 * Raw byte[] from message, possibly with a length field up front.
	 */
	@Override
	public DatagramPacket fromMessage(Message<?> message) {
		if (this.acknowledge) {
			return fromMessageWithAck(message);
		}
		byte[] bytes = getPayloadAsBytes(message);
		if (this.lengthCheck) {
			ByteBuffer buffer = ByteBuffer.allocate(bytes.length + 4);
			// insert the length (not including the length bytes)
			// default ByteOrder is	ByteOrder.BIG_ENDIAN (network byte order)
			buffer.putInt(bytes.length);
			buffer.put(bytes);
			bytes = buffer.array();
		}
		return new DatagramPacket(bytes, bytes.length);
	}

	/**
	 * Prefix raw byte[] from message with 'acknowledge to' and 'message id' "headers".
	 */
	private DatagramPacket fromMessageWithAck(Message<?> message) {
		Assert.state(StringUtils.hasText(this.ackAddress), "'ackAddress' must not be empty");
		byte[] bytes = getPayloadAsBytes(message);
		ByteBuffer buffer = ByteBuffer.allocate(100 + bytes.length);
		if (this.lengthCheck) {
			buffer.putInt(0); // placeholder for length
		}
		try {
			buffer.put(IpHeaders.ACK_ADDRESS.getBytes(this.charset));
			buffer.put((byte) '=');
			buffer.put(this.ackAddress.getBytes(this.charset));
			buffer.put((byte) ';');
			UUID id = message.getHeaders().getId();
			if (id != null) {
				buffer.put(MessageHeaders.ID.getBytes(this.charset));
				buffer.put((byte) '=');
				buffer.put(id.toString().getBytes(this.charset));
				buffer.put((byte) ';');
			}
		}
		catch (UnsupportedEncodingException e) {
			throw new MessagingException(message, "Failed to get headers", e);
		}
		int headersLength = buffer.position() - 4;
		buffer.put(bytes);
		if (this.lengthCheck) {
			// insert the length (not including the length bytes)
			// default ByteOrder is	ByteOrder.BIG_ENDIAN (network byte order)
			buffer.putInt(0, bytes.length + headersLength);
		}
		return new DatagramPacket(buffer.array(), buffer.position());
	}

	private byte[] getPayloadAsBytes(Message<?> message) {
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
				throw new UncheckedIOException(e);
			}
		}
		else {
			throw new IllegalArgumentException("The datagram packet mapper expects " +
					"either a byte array or String payload, but received: " + payload.getClass());
		}
		return bytes;
	}

	@Override
	@Nullable
	public Message<byte[]> toMessage(DatagramPacket object) {
		return toMessage(object, null);
	}

	@Override
	@Nullable
	public Message<byte[]> toMessage(DatagramPacket packet, @Nullable Map<String, Object> headers) {
		int offset = packet.getOffset();
		int length = packet.getLength();
		byte[] payload;
		ByteBuffer buffer = ByteBuffer.wrap(packet.getData(), offset, length);
		Message<byte[]> message = null;
		if (this.lengthCheck) {
			int declaredLength = buffer.getInt();
			if (declaredLength != (length - 4)) {
				throw new MessageMappingException("Incorrect length; expected " + (declaredLength + 4)
						+ ", received " + length);
			}
			offset += 4;
			length -= 4;
		}
		String hostAddress = packet.getAddress().getHostAddress();
		String hostName;
		if (this.lookupHost) {
			hostName = packet.getAddress().getHostName();
		}
		else {
			hostName = hostAddress;
		}
		int port = packet.getPort();
		// Peek at the message in case they didn't configure us for ack but the sending
		// side expects it.
		if (this.acknowledge || startsWith(buffer, IpHeaders.ACK_ADDRESS)) {
			try {
				String headersString = new String(packet.getData(), offset, length, this.charset);
				Matcher matcher = udpHeadersPattern.matcher(headersString);
				if (matcher.find()) {
					// Strip off the ack headers and put in Message headers
					length = length - matcher.end();
					payload = new byte[length];
					System.arraycopy(packet.getData(), offset + matcher.end(), payload, 0, length);
					message = getMessageBuilderFactory().withPayload(payload)
							.setHeader(IpHeaders.ACK_ID, UUID.fromString(matcher.group(2)))
							.setHeader(IpHeaders.ACK_ADDRESS, matcher.group(1))
							.setHeader(IpHeaders.HOSTNAME, hostName)
							.setHeader(IpHeaders.IP_ADDRESS, hostAddress)
							.setHeader(IpHeaders.PORT, port)
							.setHeader(IpHeaders.PACKET_ADDRESS, packet.getSocketAddress())
							.copyHeadersIfAbsent(headers)
							.build();
				}  // on no match, just treat as simple payload
			}
			catch (UnsupportedEncodingException e) {
				throw new MessageMappingException("Invalid charset", e);
			}
		}
		if (message == null) {
			payload = new byte[length];
			System.arraycopy(packet.getData(), offset, payload, 0, length);
			if (payload.length > 0) {
				message = getMessageBuilderFactory().withPayload(payload)
						.setHeader(IpHeaders.HOSTNAME, hostName)
						.setHeader(IpHeaders.IP_ADDRESS, hostAddress)
						.setHeader(IpHeaders.PORT, port)
						.setHeader(IpHeaders.PACKET_ADDRESS, packet.getSocketAddress())
						.build();
			}
		}
		return message;
	}

	/**
	 * Peeks at data in the buffer to see if starts with the prefix.
	 */
	private boolean startsWith(ByteBuffer buffer, String prefix) {
		int pos = buffer.position();
		if (buffer.limit() - pos < prefix.length()) {
			return false;
		}
		try {
			byte[] comparing;
			comparing = prefix.getBytes(this.charset);
			for (int i = 0; i < comparing.length; i++) {
				if (buffer.get() != comparing[i]) {
					return false;
				}
			}
			return true;
		}
		catch (UnsupportedEncodingException e) {
			throw new MessageMappingException("Invalid charset", e);
		}
		finally {
			//reposition the buffer
			((Buffer) buffer).position(pos);
		}
	}

}
