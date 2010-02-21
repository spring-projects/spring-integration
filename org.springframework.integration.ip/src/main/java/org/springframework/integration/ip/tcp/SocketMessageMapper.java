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

import org.springframework.integration.core.Message;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.message.InboundMessageMapper;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.OutboundMessageMapper;

/**
 * Maps incoming data from a {@link SocketReader} to a {@link Message} and from
 * a Message to outgoing data forwarded to a {@link SocketWriter}.
 * @author Gary Russell
 *
 */
public class SocketMessageMapper implements
		InboundMessageMapper<SocketReader>, 
		OutboundMessageMapper<SocketWriter> {

	/* (non-Javadoc)
	 * @see org.springframework.integration.message.InboundMessageMapper#toMessage(java.lang.Object)
	 */
	public Message<byte[]> toMessage(SocketReader socketReader) throws Exception {
		return fromRaw(socketReader);
	}


	/**
	 * @param socketReader
	 * @return
	 * @throws IOException 
	 */
	private Message<byte[]> fromRaw(SocketReader socketReader) throws IOException {
		byte[] payload = socketReader.getAssembledData();
		Message<byte[]> message = null;
		if (payload != null && payload.length > 0) {
			message = MessageBuilder.withPayload(payload)
					.setHeader(IpHeaders.HOSTNAME, socketReader.getAddress().getHostName())
					.setHeader(IpHeaders.IP_ADDRESS, socketReader.getAddress().getHostAddress())
					.build();
		}
		return message;
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.message.OutboundMessageMapper#fromMessage(org.springframework.integration.core.Message)
	 */
	public SocketWriter fromMessage(Message<?> message) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}


}
