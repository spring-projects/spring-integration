/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.websocket;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.SocketUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class JettyWebSocketTestServer implements InitializingBean, DisposableBean {

	private final Server jettyServer;

	private final int port;

	private final AnnotationConfigWebApplicationContext serverContext;

	public JettyWebSocketTestServer(Class<?>... serverConfigs) {
		this.port = SocketUtils.findAvailableTcpPort();
		this.jettyServer = new Server(this.port);
		this.serverContext = new AnnotationConfigWebApplicationContext();
		this.serverContext.register(serverConfigs);
		this.serverContext.refresh();

		ServletContextHandler contextHandler = new ServletContextHandler();
		ServletHolder servletHolder = new ServletHolder(new DispatcherServlet(this.serverContext));
		contextHandler.addServlet(servletHolder, "/");
		this.jettyServer.setHandler(contextHandler);
	}

	public AnnotationConfigWebApplicationContext getServerContext() {
		return this.serverContext;
	}

	public String getWsBaseUrl() {
		return "ws://localhost:" + this.port;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.jettyServer.start();
	}

	@Override
	public void destroy() throws Exception {
		if (this.jettyServer.isRunning()) {
			this.jettyServer.setStopTimeout(0);
			this.jettyServer.stop();
		}
	}

}
