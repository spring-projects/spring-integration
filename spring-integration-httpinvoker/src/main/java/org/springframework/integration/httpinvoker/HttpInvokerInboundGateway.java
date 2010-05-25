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

package org.springframework.integration.httpinvoker;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.integration.core.MessagingException;
import org.springframework.integration.gateway.RemotingInboundGatewaySupport;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;
import org.springframework.web.HttpRequestHandler;

/**
 * An inbound gateway adapter for HttpInvoker-based remoting. Since this class implements
 * {@link HttpRequestHandler}, it can be configured with a delegating Servlet where the
 * servlet-name matches this adapter's bean name. For example, the following servlet can
 * be defined in web.xml:
 * 
 * <pre class="code">
 * &lt;servlet&gt;
 *     &lt;servlet-name&gt;httpInvokerGateway&lt;/servlet-name&gt;
 *     &lt;servlet-class&gt;org.springframework.web.context.support.HttpRequestHandlerServlet&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 * </pre>
 * 
 * And, this would match the following bean definition in the application context loaded
 * by a {@link org.springframework.web.context.ContextLoaderListener}:
 * 
 * <pre class="code">
 * &lt;bean id="httpInvokerGateway" class="org.springframework.integration.adapter.httpinvoker.HttpInvokerGateway"&gt;
 *     &lt;constructor-arg ref="requestChannel"/&gt;
 * &lt;/bean&gt;
 * </pre>
 * 
 * <p>
 * Alternatively, in a Spring MVC application, the DispatcherServlet can delegate to the
 * "httpInvokerGateway" bean based on a handler mapping configuration. In that case,
 * the HttpRequestHandlerServlet would not be necessary.
 * </p>
 * 
 * @author Mark Fisher
 * 
 * @deprecated as of 2.0.x. We recommend using the REST-based HTTP adapters instead.
 */
@Deprecated
public class HttpInvokerInboundGateway extends RemotingInboundGatewaySupport implements HttpRequestHandler {

	private volatile HttpInvokerServiceExporter exporter;

	private final Object initializationMonitor = new Object();


	@Override
	protected void onInit() throws Exception {
		synchronized (this.initializationMonitor) {
			if (this.exporter == null) {
				HttpInvokerServiceExporter exporter = new HttpInvokerServiceExporter();
				exporter.setService(this);
				exporter.setServiceInterface(RequestReplyExchanger.class);
				exporter.afterPropertiesSet();
				this.exporter = exporter;
			}
		}
		super.onInit();
	}

	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (this.exporter == null) {
			throw new MessagingException("adapter has not been initialized");
		}
		this.exporter.handleRequest(request, response);
	}

}
