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

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class TcpConfigOutboundGatewayTests {

	static AbstractApplicationContext staticContext;

	@Autowired
	AbstractApplicationContext ctx;
	
	@Autowired
	@Qualifier(value="crLfServer")
	AbstractServerConnectionFactory crLfServer;
	
	@Autowired
	@Qualifier(value="stxEtxServer")
	AbstractServerConnectionFactory stxEtxServer;
	
	@Autowired
	@Qualifier(value="lengthHeaderServer")
	AbstractServerConnectionFactory lengthHeaderServer;
	
	@Autowired
	@Qualifier(value="javaSerialServer")
	AbstractServerConnectionFactory javaSerialServer;
	
	@Autowired	@Qualifier(value="crLfClient")
	AbstractClientConnectionFactory crLfClient;
	
	@Autowired
	@Qualifier(value="stxEtxClient")
	AbstractClientConnectionFactory stxEtxClient;
	
	@Autowired
	@Qualifier(value="lengthHeaderClient")
	AbstractClientConnectionFactory lengthHeaderClient;
	
	@Autowired
	@Qualifier(value="javaSerialClient")
	AbstractClientConnectionFactory javaSerialClient;
	
	@Autowired
	@Qualifier(value="gatewayCrLf")
	TcpInboundGateway gatewayCrLf;
	
//	@Autowired
//	@Qualifier("gatewayCrLf")
//	private TcpInboundGateway inboundGatewayCrLf;
	
	@Autowired
	@Qualifier("gatewayStxEtx")
	private TcpInboundGateway inboundGatewayStxEtx;
	
	@Autowired
	@Qualifier("gatewayLength")
	private TcpInboundGateway inboundGatewayLength;

	@Autowired
	@Qualifier("gatewaySerialized")
	private TcpInboundGateway inboundGatewaySerialized;

	@Autowired
	@Qualifier("requestChannel")
	SubscribableChannel requestChannel;

	@Autowired
	@Qualifier("replyChannel")
	PollableChannel replyChannel;

	@Autowired
	@Qualifier("requestChannelNio")
	SubscribableChannel requestChannelNio;

	@Test
	public void testOutboundCrLf() throws Exception {
		testOutboundUsingConfig();
	}

	@Test
	public void testOutboundCrLfNio() throws Exception {
		testOutboundUsingConfigNio();
	}

	private void waitListening(TcpInboundGateway gateway) throws Exception {
		int n = 0;
		while (!gateway.isListening()) {
			Thread.sleep(100);
			if (n++ > 100) {
				throw new Exception("Gateway failed to listen");
			}
		}
		
	}

	@Test
	public void testOutboundStxEtx() throws Exception {
		TcpOutboundGateway gateway = new TcpOutboundGateway();
		stxEtxClient.start();
		gateway.setConnectionFactory(stxEtxClient);
		waitListening(inboundGatewayStxEtx);
		Message<String> message = MessageBuilder.withPayload("test").build();
		@SuppressWarnings("unchecked")
		byte[] bytes = ((Message<byte[]>) gateway.handleRequestMessage(message)).getPayload();
		assertEquals("echo:test", new String(bytes));
	}

	@Test
	public void testOutboundSerialized() throws Exception {
		TcpOutboundGateway gateway = new TcpOutboundGateway();
		javaSerialClient.start();
		gateway.setConnectionFactory(javaSerialClient);
		waitListening(inboundGatewaySerialized);
		Message<String> message = MessageBuilder.withPayload("test").build();
		@SuppressWarnings("unchecked")
		Object response = ((Message<Object>) gateway.handleRequestMessage(message)).getPayload();
		assertEquals("echo:test", response);
	}

	@Test
	public void testOutboundLength() throws Exception {
		TcpOutboundGateway gateway = new TcpOutboundGateway();
		lengthHeaderClient.start();
		gateway.setConnectionFactory(lengthHeaderClient);
		waitListening(inboundGatewayLength);
		Message<String> message = MessageBuilder.withPayload("test").build();
		@SuppressWarnings("unchecked")
		byte[] bytes = ((Message<byte[]>) gateway.handleRequestMessage(message)).getPayload();
		assertEquals("echo:test", new String(bytes));
	}

	private void testOutboundUsingConfig() {
		Message<String> message = MessageBuilder.withPayload("test").build();
		requestChannel.send(message);
		byte[] bytes = (byte[]) replyChannel.receive().getPayload();
		assertEquals("echo:test", new String(bytes).trim());
	}

	private void testOutboundUsingConfigNio() {
		Message<String> message = MessageBuilder.withPayload("test").build();
		requestChannelNio.send(message);
		byte[] bytes = (byte[]) replyChannel.receive().getPayload();
		assertEquals("echo:test", new String(bytes).trim());
	}

	@Before
	public void copyContext() {
		if (staticContext == null) {
			staticContext = ctx;
		}
	}
	
	@AfterClass
	public static void shutDown() {
		staticContext.close();
	}

}
