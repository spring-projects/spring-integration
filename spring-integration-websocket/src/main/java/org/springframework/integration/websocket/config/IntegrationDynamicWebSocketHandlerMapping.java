/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.websocket.config;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

/**
 * The {@link AbstractHandlerMapping} implementation for dynamic WebSocket endpoint registrations in Spring Integration.
 * <p>
 * TODO until https://github.com/spring-projects/spring-framework/issues/26798
 *
 * @author Artem Bilan
 *
 * @since 5.5
 */
class IntegrationDynamicWebSocketHandlerMapping extends AbstractHandlerMapping {

	private final Map<String, HttpRequestHandler> handlerMap = new HashMap<>();

	@Override
	protected Object getHandlerInternal(HttpServletRequest request) {
		String lookupPath = initLookupPath(request);
		HttpRequestHandler httpRequestHandler = this.handlerMap.get(lookupPath);
		return httpRequestHandler != null ? new HandlerExecutionChain(httpRequestHandler) : null;
	}

	void registerHandler(String path, HttpRequestHandler httpHandler) {
		this.handlerMap.put(path, httpHandler);
	}

	void unregisterHandler(String path) {
		this.handlerMap.remove(path);
	}

}
