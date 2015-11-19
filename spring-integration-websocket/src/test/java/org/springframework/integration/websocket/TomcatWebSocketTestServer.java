/*
 * Copyright 2014-2015 the original author or authors.
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
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.websocket.server.WsContextListener;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * @author Rossen Stoyanchev
 * @author Artem Bilan
 * @since 4.1
 */
public class TomcatWebSocketTestServer implements InitializingBean, DisposableBean {

	private final Tomcat tomcatServer;

	private final AnnotationConfigWebApplicationContext serverContext;

	public TomcatWebSocketTestServer(Class<?>... serverConfigs) {
		this.tomcatServer = new Tomcat();
		this.tomcatServer.setPort(0);
		this.tomcatServer.setBaseDir(createTempDir());
		this.serverContext = new AnnotationConfigWebApplicationContext();
		this.serverContext.register(serverConfigs);

		Context context = this.tomcatServer.addContext("", System.getProperty("java.io.tmpdir"));
		context.addApplicationListener(WsContextListener.class.getName());
		Tomcat.addServlet(context, "dispatcherServlet", new DispatcherServlet(this.serverContext))
				.setAsyncSupported(true);
		context.addServletMapping("/", "dispatcherServlet");
	}

	private String createTempDir() {
		try {
			File tempFolder = File.createTempFile("tomcat.", ".workDir");
			tempFolder.delete();
			tempFolder.mkdir();
			tempFolder.deleteOnExit();
			return tempFolder.getAbsolutePath();
		}
		catch (IOException ex) {
			throw new RuntimeException("Unable to create temp directory", ex);
		}
	}

	public AnnotationConfigWebApplicationContext getServerContext() {
		return this.serverContext;
	}

	public String getWsBaseUrl() {
		return "ws://localhost:" + this.tomcatServer.getConnector().getLocalPort();
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
