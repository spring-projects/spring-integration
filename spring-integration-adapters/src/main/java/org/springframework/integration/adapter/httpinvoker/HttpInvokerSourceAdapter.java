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
 * A source channel adapter for HttpInvoker-based remoting.
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
