/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.integration.handler.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

/**
 * A holder for the configured argument resolvers.
 *
 * @author Gary Russell
 *
 * @since 5.0
 *
 */
public class HandlerMethodArgumentResolversHolder {

	private final List<HandlerMethodArgumentResolver> resolvers;

	public HandlerMethodArgumentResolversHolder(List<HandlerMethodArgumentResolver> resolvers) {
		this.resolvers = new ArrayList<>(resolvers);
	}

	public List<HandlerMethodArgumentResolver> getResolvers() {
		return Collections.unmodifiableList(this.resolvers);
	}

	public void addResolver(HandlerMethodArgumentResolver resolver) {
		this.resolvers.add(resolver);
	}

	public boolean removeResolver(HandlerMethodArgumentResolver resolver) {
		return this.resolvers.remove(resolver);
	}

}
