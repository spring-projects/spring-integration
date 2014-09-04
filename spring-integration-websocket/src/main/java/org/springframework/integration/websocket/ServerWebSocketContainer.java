package org.springframework.integration.websocket;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.TransportHandler;

/**
 * The {@link IntegrationWebSocketContainer} implementation for the {@code server}
 * {@link org.springframework.web.socket.WebSocketHandler} registration.
 * <p>
 * Registers an internal {@code IntegrationWebSocketContainer.IntegrationWebSocketHandler}
 * for provided {@link #paths} with the {@link WebSocketHandlerRegistry}.
 * <p>
 * The real registration is based on Spring Web-Socket infrastructure via {@link WebSocketConfigurer}
 * implementation of this class.
 *
 * @author Artem Bilan
 * @since 4.1
 */
public class ServerWebSocketContainer extends IntegrationWebSocketContainer implements WebSocketConfigurer {

	private final String[] paths;

	private volatile HandshakeHandler handshakeHandler;

	private volatile HandshakeInterceptor[] interceptors;

	private SockJsServiceOptions sockJsServiceOptions;

	public ServerWebSocketContainer(String... paths) {
		this.paths = paths;
	}

	public ServerWebSocketContainer setHandshakeHandler(HandshakeHandler handshakeHandler) {
		this.handshakeHandler = handshakeHandler;
		return this;
	}

	public ServerWebSocketContainer setInterceptors(HandshakeInterceptor[] interceptors) {
		this.interceptors = interceptors;
		return this;
	}


	public ServerWebSocketContainer withSockJs(SockJsServiceOptions... sockJsServiceOptions) {
		if (ObjectUtils.isEmpty(sockJsServiceOptions)) {
			this.sockJsServiceOptions = new SockJsServiceOptions();
		}
		else {
			Assert.state(sockJsServiceOptions.length == 1, "Only one 'sockJsServiceOptions' is applicable.");
			this.sockJsServiceOptions = sockJsServiceOptions[0];
		}
		return this;
	}

	public void setSockJsServiceOptions(SockJsServiceOptions sockJsServiceOptions) {
		this.sockJsServiceOptions = sockJsServiceOptions;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		WebSocketHandlerRegistration registration = registry.addHandler(this.webSocketHandler, this.paths)
				.setHandshakeHandler(this.handshakeHandler)
				.addInterceptors(this.interceptors);
		if (this.sockJsServiceOptions != null) {
			SockJsServiceRegistration sockJsServiceRegistration = registration.withSockJS();
			if (this.sockJsServiceOptions.webSocketEnabled != null) {
				sockJsServiceRegistration.setWebSocketEnabled(this.sockJsServiceOptions.webSocketEnabled);
			}
			if (this.sockJsServiceOptions.clientLibraryUrl != null) {
				sockJsServiceRegistration.setClientLibraryUrl(this.sockJsServiceOptions.clientLibraryUrl);
			}
			if (this.sockJsServiceOptions.disconnectDelay != null) {
				sockJsServiceRegistration.setDisconnectDelay(this.sockJsServiceOptions.disconnectDelay);
			}
			if (this.sockJsServiceOptions.heartbeatTime != null) {
				sockJsServiceRegistration.setHeartbeatTime(this.sockJsServiceOptions.heartbeatTime);
			}
			if (this.sockJsServiceOptions.httpMessageCacheSize != null) {
				sockJsServiceRegistration.setHttpMessageCacheSize(this.sockJsServiceOptions.httpMessageCacheSize);
			}
			if (this.sockJsServiceOptions.heartbeatTime != null) {
				sockJsServiceRegistration.setHeartbeatTime(this.sockJsServiceOptions.heartbeatTime);
			}
			if (this.sockJsServiceOptions.sessionCookieNeeded != null) {
				sockJsServiceRegistration.setSessionCookieNeeded(this.sockJsServiceOptions.sessionCookieNeeded);
			}
			if (this.sockJsServiceOptions.streamBytesLimit != null) {
				sockJsServiceRegistration.setStreamBytesLimit(this.sockJsServiceOptions.streamBytesLimit);
			}
			if (this.sockJsServiceOptions.transportHandlers != null) {
				sockJsServiceRegistration.setTransportHandlers(this.sockJsServiceOptions.transportHandlers);
			}
			if (this.sockJsServiceOptions.taskScheduler != null) {
				sockJsServiceRegistration.setTaskScheduler(this.sockJsServiceOptions.taskScheduler);
			}
			if (this.sockJsServiceOptions.messageCodec != null) {
				sockJsServiceRegistration.setMessageCodec(this.sockJsServiceOptions.messageCodec);
			}
		}

	}

	/**
	 * @see org.springframework.web.socket.config.annotation.SockJsServiceRegistration
	 */
	public static class SockJsServiceOptions {

		private TaskScheduler taskScheduler;

		private String clientLibraryUrl;

		private Integer streamBytesLimit;

		private Boolean sessionCookieNeeded;

		private Long heartbeatTime;

		private Long disconnectDelay;

		private Integer httpMessageCacheSize;

		private Boolean webSocketEnabled;

		private TransportHandler[] transportHandlers;

		private SockJsMessageCodec messageCodec;

		public SockJsServiceOptions setTaskScheduler(TaskScheduler taskScheduler) {
			this.taskScheduler = taskScheduler;
			return this;
		}

		public SockJsServiceOptions setClientLibraryUrl(String clientLibraryUrl) {
			this.clientLibraryUrl = clientLibraryUrl;
			return this;
		}

		public SockJsServiceOptions setStreamBytesLimit(int streamBytesLimit) {
			this.streamBytesLimit = streamBytesLimit;
			return this;
		}

		public SockJsServiceOptions setSessionCookieNeeded(boolean sessionCookieNeeded) {
			this.sessionCookieNeeded = sessionCookieNeeded;
			return this;
		}

		public SockJsServiceOptions setHeartbeatTime(long heartbeatTime) {
			this.heartbeatTime = heartbeatTime;
			return this;
		}

		public SockJsServiceOptions setDisconnectDelay(long disconnectDelay) {
			this.disconnectDelay = disconnectDelay;
			return this;
		}

		public SockJsServiceOptions setHttpMessageCacheSize(int httpMessageCacheSize) {
			this.httpMessageCacheSize = httpMessageCacheSize;
			return this;
		}

		public SockJsServiceOptions setWebSocketEnabled(boolean webSocketEnabled) {
			this.webSocketEnabled = webSocketEnabled;
			return this;
		}

		public SockJsServiceOptions setTransportHandlers(TransportHandler... transportHandlers) {
			this.transportHandlers = transportHandlers;
			return this;
		}

		public SockJsServiceOptions setMessageCodec(SockJsMessageCodec messageCodec) {
			this.messageCodec = messageCodec;
			return this;
		}

	}

}
