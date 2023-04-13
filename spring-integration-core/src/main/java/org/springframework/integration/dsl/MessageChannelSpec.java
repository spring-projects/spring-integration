/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.integration.dsl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.util.Assert;

/**
 *
 * @param <S> the target {@link MessageChannelSpec} implementation type.
 * @param <C> the target {@link AbstractMessageChannel} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public abstract class MessageChannelSpec<S extends MessageChannelSpec<S, C>, C extends AbstractMessageChannel>
		extends IntegrationComponentSpec<S, C>
		implements ComponentsRegistration {

	private final Map<Object, String> componentsToRegister = new LinkedHashMap<>();

	private final List<Class<?>> datatypes = new ArrayList<>();

	private final List<ChannelInterceptor> interceptors = new LinkedList<>();

	protected C channel; // NOSONAR

	private MessageConverter messageConverter;

	protected MessageChannelSpec() {
	}

	public S datatype(Class<?>... types) {
		Assert.notNull(types, "'datatypes' must not be null");
		Assert.noNullElements(types, "'datatypes' must not contain null elements");
		this.datatypes.addAll(Arrays.asList(types));
		return _this();
	}

	public S interceptor(ChannelInterceptor... interceptorArray) {
		Assert.notNull(interceptorArray, "'interceptorArray' must not be null");
		Assert.noNullElements(interceptorArray, "'interceptorArray' must not contain null elements");
		this.interceptors.addAll(Arrays.asList(interceptorArray));
		return _this();
	}

	/**
	 * Populate the {@code Wire Tap} EI Pattern specific
	 * {@link org.springframework.messaging.support.ChannelInterceptor} implementation.
	 * @param wireTapChannel the {@link MessageChannel} bean name to wire-tap.
	 * @return the current {@link MessageChannelSpec}.
	 * @see WireTapSpec
	 */
	public S wireTap(String wireTapChannel) {
		return wireTap(new WireTapSpec(wireTapChannel));
	}

	/**
	 * Populate the {@code Wire Tap} EI Pattern specific
	 * {@link org.springframework.messaging.support.ChannelInterceptor} implementation.
	 * @param wireTapChannel the {@link MessageChannel} instance to wire-tap.
	 * @return the current {@link MessageChannelSpec}.
	 * @see WireTapSpec
	 */
	public S wireTap(MessageChannel wireTapChannel) {
		return wireTap(new WireTapSpec(wireTapChannel));
	}

	/**
	 * Populate the {@code Wire Tap} EI Pattern specific
	 * {@link org.springframework.messaging.support.ChannelInterceptor} implementation.
	 * @param wireTapSpec the {@link WireTapSpec} to build {@link WireTap} instance.
	 * @return the current {@link MessageChannelSpec}.
	 * @see WireTap
	 */
	public S wireTap(WireTapSpec wireTapSpec) {
		WireTap interceptor = wireTapSpec.getObject();
		this.componentsToRegister.put(interceptor, null);
		return interceptor(interceptor);
	}

	public S messageConverter(MessageConverter converter) {
		this.messageConverter = converter;
		return _this();
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		return this.componentsToRegister;
	}

	@Override
	protected C doGet() {
		this.channel.setDatatypes(this.datatypes.toArray(new Class<?>[0]));
		this.channel.setBeanName(getId());
		this.channel.setInterceptors(this.interceptors);
		this.channel.setMessageConverter(this.messageConverter);
		return this.channel;
	}

}
