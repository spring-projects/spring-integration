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

import java.util.Collections;
import java.util.Map;

import org.springframework.expression.Expression;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * The {@link IntegrationComponentSpec} implementation for the {@link WireTap} component.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public class WireTapSpec extends IntegrationComponentSpec<WireTapSpec, WireTap> implements ComponentsRegistration {

	private final MessageChannel channel;

	private final String channelName;

	private MessageSelector selector;

	private Long timeout;

	public WireTapSpec(MessageChannel channel) {
		Assert.notNull(channel, "'channel' must not be null");
		this.channel = channel;
		this.channelName = null;
	}

	public WireTapSpec(String channelName) {
		Assert.notNull(channelName, "'channelName' must not be null");
		this.channelName = channelName;
		this.channel = null;
	}

	public WireTapSpec selector(String selectorExpression) {
		return selector(new ExpressionEvaluatingSelector(selectorExpression));
	}

	/**
	 * Specify an {@link Expression} for selector.
	 * @param selectorExpression the expression for selector.
	 * @return the current {@link WireTapSpec}
	 * @since 1.2
	 * @see WireTap#WireTap(MessageChannel, MessageSelector)
	 */
	public WireTapSpec selector(Expression selectorExpression) {
		return selector(new ExpressionEvaluatingSelector(selectorExpression));
	}

	public WireTapSpec selector(MessageSelector selector) {
		this.selector = selector;
		return this;
	}

	public WireTapSpec timeout(long timeout) {
		this.timeout = timeout;
		return this;
	}

	@Override
	protected WireTap doGet() {
		WireTap wireTap;
		if (this.channel != null) {
			wireTap = new WireTap(this.channel, this.selector);
		}
		else {
			wireTap = new WireTap(this.channelName, this.selector);
		}

		if (this.timeout != null) {
			wireTap.setTimeout(this.timeout);
		}
		return wireTap;
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		if (this.selector != null) {
			return Collections.singletonMap(this.selector, null);
		}
		else {
			return Collections.emptyMap();
		}
	}

}
