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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * The {@link AbstractUrlHandlerMapping} implementation for dynamic WebSocket endpoint registrations in Spring Integration.
 * <p>
 * TODO until https://github.com/spring-projects/spring-framework/issues/26798
 *
 * @author Artem Bilan
 *
 * @since 5.5
 */
class IntegrationDynamicWebSocketHandlerMapping extends AbstractUrlHandlerMapping {

	private final Map<String, HttpRequestHandler> handlerMap = new HashMap<>();

	private final Map<PathPattern, HttpRequestHandler> pathPatternHandlerMap = new LinkedHashMap<>();

	@Override
	protected Object getHandlerInternal(HttpServletRequest request) {
		String lookupPath = initLookupPath(request);
		HttpRequestHandler httpRequestHandler = this.handlerMap.get(lookupPath);
		if (httpRequestHandler == null && usesPathPatterns()) {
			RequestPath path = ServletRequestPathUtils.getParsedRequestPath(request);
			return lookupByPattern(path);
		}
		return httpRequestHandler != null ? new HandlerExecutionChain(httpRequestHandler) : null;
	}

	private Object lookupByPattern(RequestPath path) {
		List<PathPattern> matches = null;
		for (PathPattern pattern : this.pathPatternHandlerMap.keySet()) {
			if (pattern.matches(path.pathWithinApplication())) {
				matches = (matches != null ? matches : new ArrayList<>());
				matches.add(pattern);
			}
		}
		if (matches == null) {
			return null;
		}
		if (matches.size() > 1) {
			matches.sort(PathPattern.SPECIFICITY_COMPARATOR);
			if (logger.isTraceEnabled()) {
				logger.trace("Matching patterns " + matches);
			}
		}
		PathPattern pattern = matches.get(0);
		HttpRequestHandler handler = this.pathPatternHandlerMap.get(pattern);
		PathContainer pathWithinMapping = pattern.extractPathWithinPattern(path.pathWithinApplication());
		return buildPathExposingHandler(handler, pattern.getPatternString(), pathWithinMapping.value(), null);
	}

	void registerHandler(String path, HttpRequestHandler httpHandler) {
		this.handlerMap.put(path, httpHandler);
		PathPatternParser patternParser = getPatternParser();
		if (patternParser != null) {
			this.pathPatternHandlerMap.put(patternParser.parse(path), httpHandler);
		}
	}

	void unregisterHandler(String path) {
		this.handlerMap.remove(path);
		PathPatternParser patternParser = getPatternParser();
		if (patternParser != null) {
			this.pathPatternHandlerMap.remove(patternParser.parse(path));
		}
	}

}
