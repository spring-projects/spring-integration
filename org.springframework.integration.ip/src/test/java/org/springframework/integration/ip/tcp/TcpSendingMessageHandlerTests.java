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

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ServerSocketFactory;

import org.junit.Test;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;


/**
 * @author Gary Russell
 *
 */
public class TcpSendingMessageHandlerTests {

	@Test
	public void testNetBlocking() throws Exception {
		final int port = Utils.findAvailableServerSocket();
		final String testString = "abcdef";
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					TcpNetSendingMessageHandler handler = new TcpNetSendingMessageHandler("localhost", port);
					handler.setMessageFormat(MessageFormats.FORMAT_STX_ETX);
					handler.setBlockingWrite(true);
					Message<String> message = MessageBuilder.withPayload(testString).build();
					handler.handleMessage(message);
					Thread.sleep(1000000000L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		Socket socket = server.accept();
		InputStream is = socket.getInputStream();
		byte[] buff = new byte[testString.length() + 2];
		readFully(is, buff);
		assertEquals(MessageFormats.STX, buff[0]);
		assertEquals(testString, new String(buff, 1, testString.length()));
		assertEquals(MessageFormats.ETX, buff[testString.length() + 1]);
		server.close();
	}
	
	@Test
	public void testNetNonBlocking() throws Exception {
		final int port = Utils.findAvailableServerSocket();
		final String testString = "abcdef";
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					TcpNetSendingMessageHandler handler = new TcpNetSendingMessageHandler("localhost", port);
					handler.setMessageFormat(MessageFormats.FORMAT_STX_ETX);
					handler.setBlockingWrite(false);
					Message<String> message = MessageBuilder.withPayload(testString).build();
					handler.handleMessage(message);
					Thread.sleep(1000000000L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		Socket socket = server.accept();
		InputStream is = socket.getInputStream();
		byte[] buff = new byte[testString.length() + 2];
		readFully(is, buff);
		assertEquals(MessageFormats.STX, buff[0]);
		assertEquals(testString, new String(buff, 1, testString.length()));
		assertEquals(MessageFormats.ETX, buff[testString.length() + 1]);
		server.close();
	}

	@Test
	public void testNetCustom() throws Exception {
		final int port = Utils.findAvailableServerSocket();
		final String testString = "abcdef";
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					TcpNetSendingMessageHandler handler = new TcpNetSendingMessageHandler("localhost", port);
					handler.setMessageFormat(MessageFormats.FORMAT_CUSTOM);
					handler.setBlockingWrite(true);
					handler.setCustomSocketWriterClassName("org.springframework.integration.ip.tcp.CustomNetSocketWriter");
					Message<String> message = MessageBuilder.withPayload(testString).build();
					handler.handleMessage(message);
					Thread.sleep(1000000000L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		Socket socket = server.accept();
		InputStream is = socket.getInputStream();
		byte[] buff = new byte[24];
		readFully(is, buff);
		assertEquals((testString + "                        ").substring(0, 24), 
				new String(buff));
		server.close();
	}
	

	@Test
	public void testNioBlocking() throws Exception {
		final int port = Utils.findAvailableServerSocket();
		final String testString = "abcdef";
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					TcpNioSendingMessageHandler handler = new TcpNioSendingMessageHandler("localhost", port);
					handler.setMessageFormat(MessageFormats.FORMAT_STX_ETX);
					handler.setBlockingWrite(true);
					Message<String> message = MessageBuilder.withPayload(testString).build();
					handler.handleMessage(message);
					Thread.sleep(1000000000L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		Socket socket = server.accept();
		InputStream is = socket.getInputStream();
		byte[] buff = new byte[testString.length() + 2];
		readFully(is, buff);
		assertEquals(MessageFormats.STX, buff[0]);
		assertEquals(testString, new String(buff, 1, testString.length()));
		assertEquals(MessageFormats.ETX, buff[testString.length() + 1]);
		server.close();
	}
	
	@Test
	public void testNioNonBlocking() throws Exception {
		final int port = Utils.findAvailableServerSocket();
		final String testString = "abcdef";
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					TcpNioSendingMessageHandler handler = new TcpNioSendingMessageHandler("localhost", port);
					handler.setMessageFormat(MessageFormats.FORMAT_STX_ETX);
					handler.setBlockingWrite(false);
					Message<String> message = MessageBuilder.withPayload(testString).build();
					handler.handleMessage(message);
					Thread.sleep(1000000000L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		Socket socket = server.accept();
		InputStream is = socket.getInputStream();
		byte[] buff = new byte[testString.length() + 2];
		readFully(is, buff);
		assertEquals(MessageFormats.STX, buff[0]);
		assertEquals(testString, new String(buff, 1, testString.length()));
		assertEquals(MessageFormats.ETX, buff[testString.length() + 1]);
		server.close();
	}
	
	@Test
	public void testNioBlockingDirect() throws Exception {
		final int port = Utils.findAvailableServerSocket();
		final String testString = "abcdef";
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					TcpNioSendingMessageHandler handler = new TcpNioSendingMessageHandler("localhost", port);
					handler.setMessageFormat(MessageFormats.FORMAT_STX_ETX);
					handler.setBlockingWrite(true);
					handler.setUsingDirectBuffers(true);
					Message<String> message = MessageBuilder.withPayload(testString).build();
					handler.handleMessage(message);
					Thread.sleep(1000000000L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		Socket socket = server.accept();
		InputStream is = socket.getInputStream();
		byte[] buff = new byte[testString.length() + 2];
		readFully(is, buff);
		assertEquals(MessageFormats.STX, buff[0]);
		assertEquals(testString, new String(buff, 1, testString.length()));
		assertEquals(MessageFormats.ETX, buff[testString.length() + 1]);
		server.close();
	}
	
	@Test
	public void testNioNonBlockingDirect() throws Exception {
		final int port = Utils.findAvailableServerSocket();
		final String testString = "abcdef";
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					TcpNioSendingMessageHandler handler = new TcpNioSendingMessageHandler("localhost", port);
					handler.setMessageFormat(MessageFormats.FORMAT_STX_ETX);
					handler.setBlockingWrite(false);
					handler.setUsingDirectBuffers(true);
					Message<String> message = MessageBuilder.withPayload(testString).build();
					handler.handleMessage(message);
					Thread.sleep(1000000000L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		Socket socket = server.accept();
		InputStream is = socket.getInputStream();
		byte[] buff = new byte[testString.length() + 2];
		readFully(is, buff);
		assertEquals(MessageFormats.STX, buff[0]);
		assertEquals(testString, new String(buff, 1, testString.length()));
		assertEquals(MessageFormats.ETX, buff[testString.length() + 1]);
		server.close();
	}
	
	@Test
	public void testNioCustom() throws Exception {
		final int port = Utils.findAvailableServerSocket();
		final String testString = "abcdef";
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					TcpNioSendingMessageHandler handler = new TcpNioSendingMessageHandler("localhost", port);
					handler.setMessageFormat(MessageFormats.FORMAT_CUSTOM);
					handler.setBlockingWrite(true);
					handler.setCustomSocketWriteriClassName("org.springframework.integration.ip.tcp.CustomNioSocketWriter");
					Message<String> message = MessageBuilder.withPayload(testString).build();
					handler.handleMessage(message);
					Thread.sleep(1000000000L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		Socket socket = server.accept();
		InputStream is = socket.getInputStream();
		byte[] buff = new byte[24];
		readFully(is, buff);
		assertEquals((testString + "                        ").substring(0, 24), 
				new String(buff));
		server.close();
	}
	
	/**
	 * @param is
	 * @param buff
	 */
	private void readFully(InputStream is, byte[] buff) throws IOException {
		for (int i = 0; i < buff.length; i++) {
			buff[i] = (byte) is.read();
		}
	}
}
