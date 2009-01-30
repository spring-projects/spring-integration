/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.integration.gateway.SimpleMessagingGateway;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.web.HttpRequestHandler;

/**
 * @author Mark Fisher
 * @since 1.0.2
 */
public class HttpInboundGateway extends SimpleMessagingGateway implements HttpRequestHandler {

	public void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		MessageBuilder<?> builder = MessageBuilder.withPayload(request.getParameterMap());
		this.populateHeaders(request, builder);
		Object result = this.sendAndReceive(builder.build());
		if (result instanceof String) {
			response.getWriter().print((String) result);
			response.flushBuffer();
		}
		else if (result instanceof byte[]) {
			response.getOutputStream().write((byte[]) result);
			response.flushBuffer();
		}
		else {
			throw new ServletException("invalid response type [" + result.getClass().getName() + "]"); 
		}
	}

	private void populateHeaders(HttpServletRequest request, MessageBuilder<?> builder) {
		Enumeration<?> headerNames = request.getHeaderNames();
		if (headerNames != null) {
			while (headerNames.hasMoreElements()) {
				String headerName = (String) headerNames.nextElement();
				Enumeration<?> headerEnum = request.getHeaders(headerName);
				if (headerEnum != null) {
					List<Object> headers = new ArrayList<Object>();
					while (headerEnum.hasMoreElements()) {
						headers.add(headerEnum.nextElement());
					}
					if (headers.size() == 1) {
						builder.setHeader(headerName, headers.get(0));
					}
					else if (headers.size() > 1) {
						builder.setHeader(headerName, headers);
					}
				}
			}
		}
	}

}
