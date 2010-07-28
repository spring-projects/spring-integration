package org.springframework.integration.ip.udp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetSocketAddress;

import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.ip.util.SocketUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;


public class UdpChannelAdapterTests {

	@SuppressWarnings("unchecked")
	@Test
	public void testUnicastReceiver() throws Exception {
		QueueChannel channel = new QueueChannel(2);
		int port = SocketUtils.findAvailableUdpSocket();
		UnicastReceivingChannelAdapter adapter = new UnicastReceivingChannelAdapter(port);
		adapter.setOutputChannel(channel);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		adapter.setTaskScheduler(taskScheduler);
//		SocketUtils.setLocalNicIfPossible(adapter);
		adapter.start();
		SocketUtils.waitListening(adapter);
		
		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
		DatagramPacket packet = mapper.fromMessage(message);
		packet.setSocketAddress(new InetSocketAddress("localhost", port));
		new DatagramSocket(SocketUtils.findAvailableUdpSocket()).send(packet);
		Message<byte[]> receivedMessage = (Message<byte[]>) channel.receive(2000);
		assertEquals(new String(message.getPayload()), new String(receivedMessage.getPayload()));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testUnicastSender() throws Exception {
		QueueChannel channel = new QueueChannel(2);
		int port = SocketUtils.findAvailableUdpSocket();
		UnicastReceivingChannelAdapter adapter = new UnicastReceivingChannelAdapter(port);
		adapter.setOutputChannel(channel);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		adapter.setTaskScheduler(taskScheduler);
//		SocketUtils.setLocalNicIfPossible(adapter);
		adapter.start();
		SocketUtils.waitListening(adapter);

//		String whichNic = SocketUtils.chooseANic(false);
		UnicastSendingMessageHandler handler = new UnicastSendingMessageHandler(
				"localhost", port, false, true,
				"localhost",
//				whichNic, 
				SocketUtils.findAvailableUdpSocket(), 5000);
//		handler.setLocalAddress(whichNic);
		handler.afterPropertiesSet();
		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		handler.handleMessage(message);
		Message<byte[]> receivedMessage = (Message<byte[]>) channel.receive(2000);
		assertEquals(new String(message.getPayload()), new String(receivedMessage.getPayload()));
	}

	@SuppressWarnings("unchecked")
	@Test @Ignore
	public void testMulticastReceiver() throws Exception {
		QueueChannel channel = new QueueChannel(2);
		int port = SocketUtils.findAvailableUdpSocket();
		MulticastReceivingChannelAdapter adapter = new MulticastReceivingChannelAdapter("225.6.7.8", port);
		adapter.setOutputChannel(channel);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		adapter.setTaskScheduler(taskScheduler);
		String nic = SocketUtils.chooseANic(true);
		if (nic == null) {	// no multicast support
			LogFactory.getLog(this.getClass()).error("No Multicast support");
			return;
		}
		adapter.setLocalAddress(nic);
		adapter.start();
		SocketUtils.waitListening(adapter);
		
		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
		DatagramPacket packet = mapper.fromMessage(message);
		packet.setSocketAddress(new InetSocketAddress("225.6.7.8", port));
		new DatagramSocket(0, Inet4Address.getByName(nic)).send(packet);

		Message<byte[]> receivedMessage = (Message<byte[]>) channel.receive(2000);
		assertNotNull(receivedMessage);
		assertEquals(new String(message.getPayload()), new String(receivedMessage.getPayload()));
	}

	@SuppressWarnings("unchecked")
	@Test @Ignore
	public void testMulticastSender() throws Exception {
		QueueChannel channel = new QueueChannel(2);
		int port = SocketUtils.findAvailableUdpSocket();
		UnicastReceivingChannelAdapter adapter = new MulticastReceivingChannelAdapter("225.6.7.9", port);
		adapter.setOutputChannel(channel);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		adapter.setTaskScheduler(taskScheduler);
		String nic = SocketUtils.chooseANic(true);
		if (nic == null) {	// no multicast support
			LogFactory.getLog(this.getClass()).error("No Multicast support");
			return;
		}
		adapter.setLocalAddress(nic);
		adapter.start();
		SocketUtils.waitListening(adapter);
		
		MulticastSendingMessageHandler handler = new MulticastSendingMessageHandler("225.6.7.9", port);
		handler.setLocalAddress(nic);
		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		handler.handleMessage(message);
		
		Message<byte[]> receivedMessage = (Message<byte[]>) channel.receive(2000);
		assertNotNull(receivedMessage);
		assertEquals(new String(message.getPayload()), new String(receivedMessage.getPayload()));
	}


}
