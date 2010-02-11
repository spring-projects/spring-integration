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

package org.springframework.integration.ip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.BeanFactoryChannelResolver;
import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.StringMessage;

/**
 * Sends and receives a simple message through to the Udp channel adapters.
 * If run as a JUnit just sends one message and terminates (see console).
 * TODO: Use a custom output stream and catch output to verify.
 * 
 * If run from main(),  
 * hangs around for a couple of minutes to allow console interaction (enter a message on the
 * console and you should see it go through the outbound context, over UDP, and 
 * received in the other context (and written back to the console).
 *  
 * @author Gary Russell
 * @since 2.0
 */
public class TestIpMulticastEndToEnd implements Runnable {

	private String testingIpText;

	private String stdOutput;

	private CountDownLatch sentFirst = new CountDownLatch(1);

	private CountDownLatch firstReceived = new CountDownLatch(1);

	private CountDownLatch doneProcessing = new CountDownLatch(1);

	private boolean okToRun = true;

	private static long hangAroundFor = 0;


	@Test
	@Ignore
	public void runIt() throws Exception {
		TestIpMulticastEndToEnd launcher = new TestIpMulticastEndToEnd();
		Thread t = new Thread(launcher);
		t.start(); // launch the receiver
		AbstractApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"testIp-out-multicast-context.xml",
				TestIpMulticastEndToEnd.class);	
		launcher.launchSender(applicationContext);
		applicationContext.stop();
	}


	public void launchSender(ApplicationContext applicationContext) throws Exception {
		ChannelResolver channelResolver = new BeanFactoryChannelResolver(applicationContext);
		MessageChannel inputChannel = channelResolver.resolveChannelName("mcInputChannel");
		try {
			testingIpText = ">>>>>>> Testing IP (multicast) " + new Date();
			inputChannel.send(new StringMessage(testingIpText));
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
		assertTrue(firstReceived.await(2, TimeUnit.SECONDS));
		assertEquals(testingIpText, stdOutput);
	}


	/**
	 * Instantiate the receiving context
	 */
	public void run() {
		AbstractApplicationContext ctx = new ClassPathXmlApplicationContext(
				"testIp-in-multicast-context.xml",
				TestIpMulticastEndToEnd.class);
		while (okToRun) {
			try {
				sentFirst.await();
				// wait another second to allow for the asynch handoffs
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			StdOutCatcher out = ctx.getBean(StdOutCatcher.class);
			stdOutput = out.getContent();
			firstReceived.countDown();
			try {
				doneProcessing.await();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		ctx.stop();
	}


	public static void main(String[] args) throws Exception {
		hangAroundFor = 120000;
		new TestIpMulticastEndToEnd().runIt();
	}

}
