/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.adapter.httpinvoker;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.integration.adapter.AbstractMessageHandlingSourceAdapter;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;
import org.springframework.util.Assert;
import org.springframework.web.HttpRequestHandler;

/**
 * A source channel adapter for HttpInvoker-based remoting. Since this class implements
 * {@link HttpRequestHandler}, it can be configured with a delegating Servlet where the
 * servlet-name matches this adapter's bean name. For example, the following servlet can
 * be defined in web.xml:
 * 
 * <pre class="code">
 * &lt;servlet&gt;
 *     &lt;servlet-name&gt;httpInvokerSourceAdapter&lt;/servlet-name&gt;
 *     &lt;servlet-class&gt;org.springframework.web.context.support.HttpRequestHandlerServlet&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 * </pre>
 * 
 * And, this would match the following bean definition in the application context loaded
 * by a {@link org.springframework.web.contextContextLoaderListener}:
 * 
 * <pre class="code">
 * &lt;bean id="httpInvokerSourceAdapter" class="org.springframework.integration.adapter.httpinvoker.HttpInvokerSourceAdapter"&gt;
 *     &lt;constructor-arg ref="exampleChannel"/&gt;
 * &lt;/bean&gt;
 * </pre>
 * 
 * <p>
 * Alternatively, in a Spring MVC application, the DispatcherServlet can delegate to the
 * "httpInvokerSourceAdapter" bean based on a handler mapping configuration. In that case,
 * the HttpRequestHandlerServlet would not be necessary.
 * </p>
 * 
 * @author Mark Fisher
 */
public class HttpInvokerSourceAdapter extends AbstractMessageHandlingSourceAdapter implements HttpRequestHandler {

	private volatile HttpInvokerServiceExporter exporter;


	public HttpInvokerSourceAdapter(MessageChannel channel) {
		Assert.notNull(channel, "'channel' must not be null");
		this.setChannel(channel);
	}


	public void initialize() {
		HttpInvokerServiceExporter exporter = new HttpInvokerServiceExporter();
		exporter.setService(this);
		exporter.setServiceInterface(MessageHandler.class);
		exporter.afterPropertiesSet();
		this.exporter = exporter;
	}

	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (this.exporter == null) {
			throw new MessageHandlingException("adapter has not been initialized");
		}
		this.exporter.handleRequest(request, response);
	}

}
