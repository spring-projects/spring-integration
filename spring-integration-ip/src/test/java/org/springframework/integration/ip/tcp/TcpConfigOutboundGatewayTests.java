/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.ip.tcp;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class TcpConfigOutboundGatewayTests {

	private static boolean initializedFactories;

	@Autowired
	AbstractApplicationContext ctx;

	@Autowired
	@Qualifier("crLfServer")
	AbstractServerConnectionFactory crLfServer;

	@Autowired
	@Qualifier("stxEtxServer")
	AbstractServerConnectionFactory stxEtxServer;

	@Autowired
	@Qualifier("lengthHeaderServer")
	AbstractServerConnectionFactory lengthHeaderServer;

	@Autowired
	@Qualifier("javaSerialServer")
	AbstractServerConnectionFactory javaSerialServer;

	@Autowired
	@Qualifier("crLfClient")
	AbstractClientConnectionFactory crLfClient;

	@Autowired
	@Qualifier("stxEtxClient")
	AbstractClientConnectionFactory stxEtxClient;

	@Autowired
	@Qualifier("lengthHeaderClient")
	AbstractClientConnectionFactory lengthHeaderClient;

	@Autowired
	@Qualifier("javaSerialClient")
	AbstractClientConnectionFactory javaSerialClient;

	@Autowired
	@Qualifier("gatewayCrLf")
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

	@Autowired
	MessageChannel tcpOutboundGatewayInsideChain;

	@Before
	public void before() {
		if (initializedFactories) {
			return;
		}
		Map<String, AbstractServerConnectionFactory> servers =
				this.ctx.getBeansOfType(AbstractServerConnectionFactory.class);
		servers.forEach((k, v) -> {
			TestingUtilities.waitListening(v, null);
			switch (k) {
				case "crLfServer":
					this.crLfClient.setPort(v.getPort());
					break;
				case "crLfServer2":
					this.ctx.getBean("crLfClient2", AbstractClientConnectionFactory.class).setPort(v.getPort());
					break;
				case "crLfServerNio":
					this.ctx.getBean("crLfClientNio", AbstractClientConnectionFactory.class).setPort(v.getPort());
					break;
				case "stxEtxServer":
					this.stxEtxClient.setPort(v.getPort());
					break;
				case "stxEtxServerNio":
					this.ctx.getBean("stxEtxClientNio", AbstractClientConnectionFactory.class).setPort(v.getPort());
					break;
				case "lengthHeaderServer":
					this.lengthHeaderClient.setPort(v.getPort());
					break;
				case "lengthHeaderServerNio":
					this.ctx.getBean("lengthHeaderClientNio",
							AbstractClientConnectionFactory.class).setPort(v.getPort());
					break;
				case "javaSerialServer":
					this.javaSerialClient.setPort(v.getPort());
					break;
				case "javaSerialServerNio":
					this.ctx.getBean("javaSerialClientNio",
							AbstractClientConnectionFactory.class).setPort(v.getPort());
					break;
				default:
					fail("Unexpected server:" + v);
			}
		});
		Map<String, ConsumerEndpointFactoryBean> consumers =
				this.ctx.getBeansOfType(ConsumerEndpointFactoryBean.class);
		consumers.values().forEach(g -> g.start());
		initializedFactories = true;
	}

	@Test
	public void testOutboundCrLf() throws Exception {
		testOutboundUsingConfig();
	}

	@Test
	public void testOutboundCrLfNio() throws Exception {
		testOutboundUsingConfigNio();
	}

	@Test
	public void testOutboundStxEtx() throws Exception {
		TcpOutboundGateway gateway = new TcpOutboundGateway();
		stxEtxClient.start();
		gateway.setConnectionFactory(stxEtxClient);
		Message<String> message = MessageBuilder.withPayload("test").build();
		@SuppressWarnings("unchecked")
		byte[] bytes = ((Message<byte[]>) gateway.handleRequestMessage(message)).getPayload();
		assertThat(new String(bytes)).isEqualTo("echo:test");
	}

	@Test
	public void testOutboundSerialized() throws Exception {
		TcpOutboundGateway gateway = new TcpOutboundGateway();
		javaSerialClient.start();
		gateway.setConnectionFactory(javaSerialClient);
		Message<String> message = MessageBuilder.withPayload("test").build();
		@SuppressWarnings("unchecked")
		Object response = ((Message<Object>) gateway.handleRequestMessage(message)).getPayload();
		assertThat(response).isEqualTo("echo:test");
	}

	@Test
	public void testOutboundLength() throws Exception {
		TcpOutboundGateway gateway = new TcpOutboundGateway();
		lengthHeaderClient.start();
		gateway.setConnectionFactory(lengthHeaderClient);
		Message<String> message = MessageBuilder.withPayload("test").build();
		@SuppressWarnings("unchecked")
		byte[] bytes = ((Message<byte[]>) gateway.handleRequestMessage(message)).getPayload();
		assertThat(new String(bytes)).isEqualTo("echo:test");
	}

	@Test //INT-1029
	public void testOutboundInsideChain() throws Exception {
//		this.ctx.getBean("tcp-outbound-gateway-within-chain.handler", TcpOutboundGateway.class);
		tcpOutboundGatewayInsideChain.send(MessageBuilder.withPayload("test").build());
		byte[] bytes = (byte[]) replyChannel.receive().getPayload();
		assertThat(new String(bytes).trim()).isEqualTo("echo:test");
	}

	private void testOutboundUsingConfig() {
		Message<String> message = MessageBuilder.withPayload("test").build();
		requestChannel.send(message);
		byte[] bytes = (byte[]) replyChannel.receive().getPayload();
		assertThat(new String(bytes).trim()).isEqualTo("echo:test");
	}

	private void testOutboundUsingConfigNio() {
		Message<String> message = MessageBuilder.withPayload("test").build();
		requestChannelNio.send(message);
		byte[] bytes = (byte[]) replyChannel.receive().getPayload();
		assertThat(new String(bytes).trim()).isEqualTo("echo:test");
	}

}
