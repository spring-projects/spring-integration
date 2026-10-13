/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.ftp.session;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;

/**
 * A minimal proxy server for tests, which records the targets of the tunneled connections.
 * Either a SOCKS5 proxy without authentication, or an HTTP proxy with the {@code CONNECT}
 * method and an optional basic authentication.
 *
 * @author Sharang Gupta
 */
abstract class LocalProxyServer implements AutoCloseable {

	private final ServerSocket serverSocket;

	private final List<String> tunnels = Collections.synchronizedList(new ArrayList<>());

	private final List<Socket> sockets = Collections.synchronizedList(new ArrayList<>());

	LocalProxyServer() throws IOException {
		this.serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
		Thread acceptor = new Thread(this::acceptConnections, "local-proxy-acceptor");
		acceptor.setDaemon(true);
		acceptor.start();
	}

	static LocalProxyServer socks5() throws IOException {
		return new Socks5ProxyServer();
	}

	static LocalProxyServer httpConnect(@Nullable String username, @Nullable String password) throws IOException {
		return new HttpConnectProxyServer(username, password);
	}

	InetSocketAddress address() {
		return new InetSocketAddress(this.serverSocket.getInetAddress(), this.serverSocket.getLocalPort());
	}

	/**
	 * Return the {@code host:port} targets of the tunneled connections, in the order of the connections.
	 * @return the targets.
	 */
	List<String> tunnels() {
		return new ArrayList<>(this.tunnels);
	}

	/**
	 * Perform the handshake with the client and return the socket to the target, or {@code null}
	 * when the connection was refused.
	 * @param client the socket of the client.
	 * @return the socket to the target.
	 * @throws IOException an IO failure.
	 */
	abstract @Nullable Socket handshake(Socket client) throws IOException;

	Socket connectToTarget(String host, int port) throws IOException {
		this.tunnels.add(host + ":" + port);
		Socket target = new Socket(host, port);
		this.sockets.add(target);
		return target;
	}

	private void acceptConnections() {
		while (!this.serverSocket.isClosed()) {
			try {
				Socket client = this.serverSocket.accept();
				this.sockets.add(client);
				Thread handler = new Thread(() -> handle(client), "local-proxy-handler");
				handler.setDaemon(true);
				handler.start();
			}
			catch (IOException ex) {
				return;
			}
		}
	}

	private void handle(Socket client) {
		try {
			Socket target = handshake(client);
			if (target == null) {
				client.close();
				return;
			}
			relay(client, target);
		}
		catch (IOException ex) {
			closeQuietly(client);
		}
	}

	private static void relay(Socket client, Socket target) throws IOException {
		AtomicInteger openDirections = new AtomicInteger(2);
		pump(client.getInputStream(), target, client, openDirections);
		pump(target.getInputStream(), client, target, openDirections);
	}

	private static void pump(InputStream from, Socket to, Socket other, AtomicInteger openDirections) {
		Thread thread = new Thread(() -> {
			try {
				from.transferTo(to.getOutputStream());
				to.shutdownOutput();
			}
			catch (IOException ex) {
				// the connection was closed by one of the sides
			}
			if (openDirections.decrementAndGet() == 0) {
				closeQuietly(to);
				closeQuietly(other);
			}
		}, "local-proxy-pump");
		thread.setDaemon(true);
		thread.start();
	}

	private static void closeQuietly(Socket socket) {
		try {
			socket.close();
		}
		catch (IOException ex) {
			// ignore
		}
	}

	@Override
	public void close() throws IOException {
		this.serverSocket.close();
		synchronized (this.sockets) {
			this.sockets.forEach(LocalProxyServer::closeQuietly);
		}
	}

	private static final class Socks5ProxyServer extends LocalProxyServer {

		private static final int VERSION = 5;

		private static final int NO_AUTHENTICATION = 0;

		private static final int CONNECT = 1;

		private static final int IPV4 = 1;

		private static final int DOMAIN_NAME = 3;

		private static final int IPV6 = 4;

		Socks5ProxyServer() throws IOException {
		}

		@Override
		@Nullable Socket handshake(Socket client) throws IOException {
			DataInputStream in = new DataInputStream(client.getInputStream());
			OutputStream out = client.getOutputStream();
			if (in.readUnsignedByte() != VERSION) {
				return null;
			}
			int methods = in.readUnsignedByte();
			in.readFully(new byte[methods]);
			out.write(new byte[] {VERSION, NO_AUTHENTICATION});
			out.flush();

			in.readUnsignedByte(); // the version
			int command = in.readUnsignedByte();
			in.readUnsignedByte(); // reserved
			int addressType = in.readUnsignedByte();
			String host = switch (addressType) {
				case IPV4 -> readAddress(in, 4);
				case DOMAIN_NAME -> {
					byte[] name = new byte[in.readUnsignedByte()];
					in.readFully(name);
					yield new String(name, StandardCharsets.US_ASCII);
				}
				case IPV6 -> readAddress(in, 16);
				default -> throw new IOException("Unsupported address type: " + addressType);
			};
			int port = in.readUnsignedShort();
			if (command != CONNECT) {
				throw new IOException("Unsupported command: " + command);
			}
			Socket target = connectToTarget(host, port);
			out.write(new byte[] {VERSION, 0, 0, IPV4, 0, 0, 0, 0, 0, 0});
			out.flush();
			return target;
		}

		private static String readAddress(DataInputStream in, int length) throws IOException {
			byte[] address = new byte[length];
			in.readFully(address);
			return InetAddress.getByAddress(address).getHostAddress();
		}

	}

	private static final class HttpConnectProxyServer extends LocalProxyServer {

		private final @Nullable String expectedAuthorization;

		HttpConnectProxyServer(@Nullable String username, @Nullable String password) throws IOException {
			this.expectedAuthorization = username != null
					? "Basic " + Base64.getEncoder()
							.encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8))
					: null;
		}

		@Override
		@Nullable Socket handshake(Socket client) throws IOException {
			BufferedReader in = new BufferedReader(
					new InputStreamReader(client.getInputStream(), StandardCharsets.ISO_8859_1));
			OutputStream out = client.getOutputStream();
			String requestLine = in.readLine();
			String authorization = null;
			for (String header = in.readLine(); header != null && !header.isEmpty(); header = in.readLine()) {
				if (header.regionMatches(true, 0, "Proxy-Authorization:", 0, "Proxy-Authorization:".length())) {
					authorization = header.substring("Proxy-Authorization:".length()).trim();
				}
			}
			if (requestLine == null || !requestLine.startsWith("CONNECT ")) {
				respond(out, "HTTP/1.1 405 Method Not Allowed\r\nContent-Length: 0\r\n\r\n");
				return null;
			}
			if (this.expectedAuthorization != null && !this.expectedAuthorization.equals(authorization)) {
				respond(out, "HTTP/1.1 407 Proxy Authentication Required\r\n"
						+ "Proxy-Authenticate: Basic realm=\"test\"\r\nContent-Length: 0\r\n\r\n");
				return null;
			}
			String hostAndPort = requestLine.split(" ")[1];
			int separator = hostAndPort.lastIndexOf(':');
			Socket target = connectToTarget(hostAndPort.substring(0, separator),
					Integer.parseInt(hostAndPort.substring(separator + 1)));
			respond(out, "HTTP/1.1 200 Connection established\r\n\r\n");
			return target;
		}

		private static void respond(OutputStream out, String response) throws IOException {
			out.write(response.getBytes(StandardCharsets.ISO_8859_1));
			out.flush();
		}

	}

}
