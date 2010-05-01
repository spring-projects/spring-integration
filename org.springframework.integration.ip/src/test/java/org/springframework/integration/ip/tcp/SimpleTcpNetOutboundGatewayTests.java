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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

/**
 * @author Gary Russell
 *
 */
@ContextConfiguration(locations="SimpleTcpNetInboundGatewayTests-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class SimpleTcpNetOutboundGatewayTests {

	@Autowired
	@Qualifier("gatewayCrLf")
	private SimpleTcpNetInboundGateway inboundGatewayCrLf;
	
	@Autowired
	@Qualifier("gatewayStxEtx")
	private SimpleTcpNetInboundGateway inboundGatewayStxEtx;
	
	@Autowired
	@Qualifier("gatewayLength")
	private SimpleTcpNetInboundGateway inboundGatewayLength;

	@Autowired
	@Qualifier("gatewayCustom")
	private SimpleTcpNetInboundGateway inboundGatewayCustom;

	@Test
	public void testOutboundCrLf() throws Exception {
		SimpleTcpNetOutboundGateway gateway = new SimpleTcpNetOutboundGateway
			("localhost", inboundGatewayCrLf.getPort());
		gateway.setMessageFormat(MessageFormats.FORMAT_CRLF);
		Message<String> message = MessageBuilder.withPayload("test").build();
		byte[] bytes = (byte[]) gateway.handleRequestMessage(message);
		assertEquals("echo:test", new String(bytes));
	}

	@Test
	public void testOutboundStxEtx() throws Exception {
		SimpleTcpNetOutboundGateway gateway = new SimpleTcpNetOutboundGateway
			("localhost", inboundGatewayStxEtx.getPort());
		gateway.setMessageFormat(MessageFormats.FORMAT_STX_ETX);
		Message<String> message = MessageBuilder.withPayload("test").build();
		byte[] bytes = (byte[]) gateway.handleRequestMessage(message);
		assertEquals("echo:test", new String(bytes));
	}

	@Test
	public void testOutboundLength() throws Exception {
		SimpleTcpNetOutboundGateway gateway = new SimpleTcpNetOutboundGateway
			("localhost", inboundGatewayLength.getPort());
		gateway.setMessageFormat(MessageFormats.FORMAT_LENGTH_HEADER);
		Message<String> message = MessageBuilder.withPayload("test").build();
		byte[] bytes = (byte[]) gateway.handleRequestMessage(message);
		assertEquals("echo:test", new String(bytes));
	}

	@Test
	public void testOutboundCustom() throws Exception {
		SimpleTcpNetOutboundGateway gateway = new SimpleTcpNetOutboundGateway
			("localhost", inboundGatewayCustom.getPort());
		gateway.setMessageFormat(MessageFormats.FORMAT_CUSTOM);
		gateway.setCustomSocketReaderClassName("org.springframework.integration.ip.tcp.CustomNetSocketReader");
		gateway.setCustomSocketWriterClassName("org.springframework.integration.ip.tcp.CustomNetSocketWriter");
		Message<String> message = MessageBuilder.withPayload("test").build();
		byte[] bytes = (byte[]) gateway.handleRequestMessage(message);
		assertEquals("echo:test", new String(bytes).trim());
	}
}
