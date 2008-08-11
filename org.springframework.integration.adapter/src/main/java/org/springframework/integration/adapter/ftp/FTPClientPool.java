package org.springframework.integration.adapter.ftp;

import java.io.IOException;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;

public class FTPClientPool {

	private static final int DEFAULT_POOL_SIZE = 5;
	
	private final Queue<FTPClient> pool;

	private FTPClientConfig config;

	private String host;

	private int port = FTP.DEFAULT_PORT;

	private String user;

	private String pass;

	private FTPClientFactory factory = new DefaultFactory();

	private final Log log = LogFactory.getLog(this.getClass());

	public FTPClientPool() {
		this(DEFAULT_POOL_SIZE);
	}

	public FTPClientPool(int maxPoolSize) {
		pool = new ArrayBlockingQueue<FTPClient>(maxPoolSize);
	}

	public synchronized FTPClient getClient() throws SocketException, IOException {
		return pool.isEmpty() ? factory.getClient() : pool.element();
	}

	public synchronized void releaseClient(FTPClient client) {
		if (client != null) {
			if (!pool.offer(client)) {
				try {
					client.disconnect();
				}
				catch (IOException e) {
					log.warn("Error disconnecting ftpclient", e);
				}
			}
		}
	}
	
	private class DefaultFactory implements FTPClientFactory {

		public FTPClient getClient() throws SocketException, IOException {
			FTPClient client = new FTPClient();
			client.configure(config);
			client.connect(host, port);
			client.login(user, pass);
			return client;
		}
	}

	public void setConfig(FTPClientConfig config) {
		this.config = config;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}

	public void setFactory(FTPClientFactory factory) {
		this.factory = factory;
	}
}
