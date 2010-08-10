package org.springframework.integration.ip.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import javax.net.SocketFactory;

import org.junit.Test;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.ChannelResolver;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;
import org.springframework.integration.ip.util.SocketUtils;


public class TcpInboundGatewayTests {

	@Test
	public void testNetSingle() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		scf.setSingleUse(true);
		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(scf);
		scf.start();
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 200) {
				fail("Failed to listen");
			}
		}
		final QueueChannel channel = new QueueChannel();
		gateway.setRequestChannel(channel);
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new Service());
		handler.setChannelResolver(new ChannelResolver() {
			public MessageChannel resolveChannelName(String channelName) {
				return channel;
			}
		});
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket.getOutputStream().write("Test2\r\n".getBytes());
		handler.handleMessage(channel.receive());
		handler.handleMessage(channel.receive());
		byte[] bytes = new byte[12];
		readFully(socket.getInputStream(), bytes);
		assertEquals("Echo:Test1\r\n", new String(bytes));
		readFully(socket.getInputStream(), bytes);
		assertEquals("Echo:Test2\r\n", new String(bytes));
	}

	@Test
	public void testNetNotSingle() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		scf.setSingleUse(false);
		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(scf);
		scf.start();
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 200) {
				fail("Failed to listen");
			}
		}
		final QueueChannel channel = new QueueChannel();
		gateway.setRequestChannel(channel);
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new Service());
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket.getOutputStream().write("Test2\r\n".getBytes());
		handler.handleMessage(channel.receive());
		handler.handleMessage(channel.receive());
		byte[] bytes = new byte[12];
		readFully(socket.getInputStream(), bytes);
		assertEquals("Echo:Test1\r\n", new String(bytes));
		readFully(socket.getInputStream(), bytes);
		assertEquals("Echo:Test2\r\n", new String(bytes));
	}

	@Test
	public void testNioSingle() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		scf.setSingleUse(true);
		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(scf);
		scf.start();
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 200) {
				fail("Failed to listen");
			}
		}
		final QueueChannel channel = new QueueChannel();
		gateway.setRequestChannel(channel);
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new Service());
		handler.setChannelResolver(new ChannelResolver() {
			public MessageChannel resolveChannelName(String channelName) {
				return channel;
			}
		});
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket.getOutputStream().write("Test2\r\n".getBytes());
		handler.handleMessage(channel.receive());
		handler.handleMessage(channel.receive());
		byte[] bytes = new byte[12];
		readFully(socket.getInputStream(), bytes);
		assertEquals("Echo:Test1\r\n", new String(bytes));
		readFully(socket.getInputStream(), bytes);
		assertEquals("Echo:Test2\r\n", new String(bytes));
	}

	@Test
	public void testNioNotSingle() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		scf.setSingleUse(false);
		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(scf);
		scf.start();
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 200) {
				fail("Failed to listen");
			}
		}
		final QueueChannel channel = new QueueChannel();
		gateway.setRequestChannel(channel);
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new Service());
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket.getOutputStream().write("Test2\r\n".getBytes());
		handler.handleMessage(channel.receive());
		handler.handleMessage(channel.receive());
		byte[] bytes = new byte[12];
		readFully(socket.getInputStream(), bytes);
		assertEquals("Echo:Test1\r\n", new String(bytes));
		readFully(socket.getInputStream(), bytes);
		assertEquals("Echo:Test2\r\n", new String(bytes));
	}

	private class Service {
		@SuppressWarnings("unused")
		public String serviceMethod(byte[] bytes) {
			return "Echo:" + new String(bytes);
		}
	}

	private void readFully(InputStream is, byte[] buff) throws IOException {
		for (int i = 0; i < buff.length; i++) {
			buff[i] = (byte) is.read();
		}
	}
	
}
