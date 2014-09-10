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

import java.io.File;
import java.io.IOException;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.websocket.server.WsContextListener;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.SocketUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * @author Rossen Stoyanchev
 * @author Artem Bilan
 * @since 4.1
 */
public class TomcatWebSocketTestServer implements InitializingBean, DisposableBean {

	private final Tomcat tomcatServer;

	private final int port;

	private final AnnotationConfigWebApplicationContext serverContext;

	public TomcatWebSocketTestServer(Class<?>... serverConfigs) {
		this.port = SocketUtils.findAvailableTcpPort();
		Connector connector = new Connector(Http11NioProtocol.class.getName());
		connector.setPort(this.port);

		File baseDir = createTempDir("tomcat");
		String baseDirPath = baseDir.getAbsolutePath();

		this.tomcatServer = new Tomcat();
		this.tomcatServer.setBaseDir(baseDirPath);
		this.tomcatServer.setPort(this.port);
		this.tomcatServer.getService().addConnector(connector);
		this.tomcatServer.setConnector(connector);

		this.serverContext = new AnnotationConfigWebApplicationContext();
		this.serverContext.register(serverConfigs);

		Context context = this.tomcatServer.addContext("", System.getProperty("java.io.tmpdir"));
		context.addApplicationListener(WsContextListener.class.getName());
		Tomcat.addServlet(context, "dispatcherServlet", new DispatcherServlet(this.serverContext)).setAsyncSupported(true);
		context.addServletMapping("/", "dispatcherServlet");
	}

	private File createTempDir(String prefix) {
		try {
			File tempFolder = File.createTempFile(prefix + ".", "." + this.port);
			tempFolder.delete();
			tempFolder.mkdir();
			tempFolder.deleteOnExit();
			return tempFolder;
		}
		catch (IOException ex) {
			throw new RuntimeException("Unable to create temp directory", ex);
		}
	}

	public AnnotationConfigWebApplicationContext getServerContext() {
		return this.serverContext;
	}

	public String getWsBaseUrl() {
		return "ws://localhost:" + this.port;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.tomcatServer.start();
	}

	@Override
	public void destroy() throws Exception {
		this.tomcatServer.stop();
	}

}
