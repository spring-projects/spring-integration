/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.websocket;

import java.io.File;
import java.io.IOException;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.websocket.server.WsContextListener;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * @author Rossen Stoyanchev
 * @author Artem Bilan
 *
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
		Wrapper dispatcherServlet =
				Tomcat.addServlet(context, "dispatcherServlet", new DispatcherServlet(this.serverContext));
		dispatcherServlet.setAsyncSupported(true);
		dispatcherServlet.setLoadOnStartup(1);
		context.addServletMappingDecoded("/", "dispatcherServlet");
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
		this.serverContext.close();
		this.tomcatServer.stop();
	}

}
