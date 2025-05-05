/*
 * Copyright 2002-2025 the original author or authors.
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

import java.time.Duration;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

/**
 * Sends and receives a simple message through to the Udp channel adapters.
 * If run as a JUnit just sends one message and terminates (see console).
 *
 * If run from main(),
 * hangs around for a couple of minutes to allow console interaction - enter a message on the
 * console and you should see it go through the outbound context, over UDP, and
 * received in the other context (and written back to the console).
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @since 2.0
 */
public class UdpUnicastEndToEndTests implements Runnable {

	private String testingIpText;

	private Message<byte[]> finalMessage;

	private CountDownLatch sentFirst = new CountDownLatch(1);

	private CountDownLatch firstReceived = new CountDownLatch(1);

	private CountDownLatch doneProcessing = new CountDownLatch(1);

	private boolean okToRun = true;

	private CountDownLatch readyToReceive = new CountDownLatch(1);

	private volatile int receiverPort;

	private static long hangAroundFor = 10;

	@BeforeEach
	public void setup() {
		this.testingIpText = null;
		this.finalMessage = null;
		this.sentFirst = new CountDownLatch(1);
		this.firstReceived = new CountDownLatch(1);
		this.doneProcessing = new CountDownLatch(1);
		this.okToRun = true;
		this.readyToReceive = new CountDownLatch(1);
	}

	@Test
	public void runIt() throws Exception {
		String location = "org/springframework/integration/ip/udp/testIp-out-context.xml";
		test(location);
	}

	@Test
	public void tesUdpOutboundChannelAdapterWithinChain() throws Exception {
		String location = "org/springframework/integration/ip/udp/testIp-out-within-chain-context.xml";
		test(location);
	}

	private void test(String location) throws InterruptedException, Exception {
		UdpUnicastEndToEndTests launcher = new UdpUnicastEndToEndTests();
		Thread t = new Thread(launcher);
		t.start(); // launch the receiver
		await("Receiver failed to listen").atMost(Duration.ofSeconds(10)).until(() -> launcher.getReceiverPort() > 0);

		ClassPathXmlApplicationContext applicationContext = createContext(launcher, location);
		launcher.launchSender(applicationContext);
		applicationContext.close();
	}

	private ClassPathXmlApplicationContext createContext(UdpUnicastEndToEndTests launcher, String location) {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
		applicationContext.setConfigLocation(location);
		StandardEnvironment env = new StandardEnvironment();
		Properties props = new Properties();
		props.setProperty("port", Integer.toString(launcher.getReceiverPort()));
		PropertiesPropertySource pps = new PropertiesPropertySource("ftpprops", props);
		env.getPropertySources().addLast(pps);
		applicationContext.setEnvironment(env);
		applicationContext.refresh();
		return applicationContext;
	}

	public void launchSender(ApplicationContext applicationContext) throws Exception {
		DestinationResolver<MessageChannel> channelResolver = new BeanFactoryChannelResolver(applicationContext);
		MessageChannel inputChannel = channelResolver.resolveDestination("inputChannel");
		if (!readyToReceive.await(30, TimeUnit.SECONDS)) {
			fail("Receiver failed to start in 30s");
		}
		try {
			testingIpText = ">>>>>>> Testing IP " + new Date();
			inputChannel.send(new GenericMessage<String>(testingIpText));
			sentFirst.countDown();
			try {
				Thread.sleep(hangAroundFor); // give some time for console interaction
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		finally {
			if (hangAroundFor == 0) {
				sentFirst = new CountDownLatch(1);
			}
			else {
				okToRun = false;
			}
			// tell the receiver to we're done
			doneProcessing.countDown();
		}
		assertThat(firstReceived.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(new String(finalMessage.getPayload())).isEqualTo(testingIpText);
	}

	public int getReceiverPort() {
		return receiverPort;
	}

	/**
	 * Instantiate the receiving context
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		@SuppressWarnings("resource")
		AbstractApplicationContext ctx = new ClassPathXmlApplicationContext(
				"testIp-in-context.xml", UdpUnicastEndToEndTests.class);
		UnicastReceivingChannelAdapter inbound = ctx.getBean(UnicastReceivingChannelAdapter.class);
		int n = 0;
		try {
			while (!inbound.isListening()) {
				Thread.sleep(100);
				if (n++ > 100) {
					throw new RuntimeException("Failed to start listening");
				}
			}
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("interrupted");
		}
		this.receiverPort = inbound.getPort();
		while (okToRun) {
			try {
				readyToReceive.countDown();
				sentFirst.await();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			QueueChannel channel = ctx.getBean("udpOutChannel", QueueChannel.class);
			finalMessage = (Message<byte[]>) channel.receive();
			MessageHistory history = MessageHistory.read(finalMessage);
			Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "udpReceiver", 0);
			assertThat(componentHistoryRecord).isNotNull();
			assertThat(componentHistoryRecord.get("type")).isEqualTo("ip:udp-inbound-channel-adapter");
			firstReceived.countDown();
			try {
				doneProcessing.await();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		ctx.stop();
		ctx.close();
	}

	public static void main(String[] args) throws Exception {
		hangAroundFor = 120000;
		new UdpUnicastEndToEndTests().runIt();
		new UdpUnicastEndToEndTests().tesUdpOutboundChannelAdapterWithinChain();
	}

}
