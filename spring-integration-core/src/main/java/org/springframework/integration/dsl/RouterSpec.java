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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.router.AbstractMappingMessageRouter;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.management.MappingMessageRouterManagement;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The {@link AbstractRouterSpec} for an {@link AbstractMappingMessageRouter}.
 *
 * @param <K> the key type.
 * @param <R> the {@link AbstractMappingMessageRouter} implementation type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class RouterSpec<K, R extends AbstractMappingMessageRouter> extends AbstractRouterSpec<RouterSpec<K, R>, R> {

	private final RouterMappingProvider mappingProvider;

	private String prefix;

	private String suffix;

	private boolean mappingProviderRegistered;

	protected RouterSpec(R router) {
		super(router);
		this.mappingProvider = new RouterMappingProvider(this.handler);
	}

	@Override
	public RouterSpec<K, R> sendTimeout(long sendTimeout) {
		this.handler.setSendTimeout(sendTimeout);
		return this;
	}

	/**
	 * @param resolutionRequired the resolutionRequired.
	 * @return the router spec.
	 * @see AbstractMappingMessageRouter#setResolutionRequired(boolean)
	 */
	public RouterSpec<K, R> resolutionRequired(boolean resolutionRequired) {
		this.handler.setResolutionRequired(resolutionRequired);
		return _this();
	}

	/**
	 * Set a limit for how many dynamic channels are retained (for reporting purposes).
	 * When the limit is exceeded, the oldest channel is discarded.
	 * <p><b>NOTE: this does not affect routing, just the reporting which dynamically
	 * resolved channels have been routed to.</b> Default {@code 100}.
	 * @param dynamicChannelLimit the limit.
	 * @return the router spec.
	 * @see AbstractMappingMessageRouter#setDynamicChannelLimit(int)
	 */
	public RouterSpec<K, R> dynamicChannelLimit(int dynamicChannelLimit) {
		this.handler.setDynamicChannelLimit(dynamicChannelLimit);
		return _this();
	}

	/**
	 * Cannot be invoked if {@link #subFlowMapping(Object, IntegrationFlow)} is used.
	 * @param prefix the prefix.
	 * @return the router spec.
	 * @see AbstractMappingMessageRouter#setPrefix(String)
	 */
	public RouterSpec<K, R> prefix(String prefix) {
		Assert.state(this.componentsToRegister.isEmpty(),
				"The 'prefix'('suffix') and 'subFlowMapping' are mutually exclusive");
		this.prefix = prefix;
		this.handler.setPrefix(prefix);
		return _this();
	}

	/**
	 * Cannot be invoked if {@link #subFlowMapping(Object, IntegrationFlow)} is used.
	 * @param suffix the suffix to set.
	 * @return the router spec.
	 * @see AbstractMappingMessageRouter#setSuffix(String)
	 */
	public RouterSpec<K, R> suffix(String suffix) {
		Assert.state(this.componentsToRegister.isEmpty(),
				"The 'prefix'('suffix') and 'subFlowMapping' are mutually exclusive");
		this.suffix = suffix;
		this.handler.setSuffix(suffix);
		return _this();
	}

	/**
	 * When true (default), if a resolved channel key does not exist in the channel map,
	 * the key itself is used as the channel name, which we will attempt to resolve to a
	 * channel. Set to {@code false} to disable this feature.
	 * @param channelKeyFallback false to disable the fallback.
	 * @return the router spec.
	 * @since 6.0
	 * @see AbstractMappingMessageRouter#setChannelKeyFallback(boolean)
	 */
	public RouterSpec<K, R> channelKeyFallback(boolean channelKeyFallback) {
		this.handler.setChannelKeyFallback(channelKeyFallback);
		return _this();
	}

	/**
	 * @param key the key.
	 * @param channelName the channelName.
	 * @return the router spec.
	 * @see AbstractMappingMessageRouter#setChannelMapping(String, String)
	 */
	public RouterSpec<K, R> channelMapping(K key, String channelName) {
		Assert.notNull(key, "'key' must not be null");
		Assert.hasText(channelName, "'channelName' must not be empty");
		if (key instanceof String) {
			this.handler.setChannelMapping((String) key, channelName);
		}
		else {
			this.mappingProvider.addMapping(key, new NamedComponent() {

				@Override
				public String getComponentName() {
					return channelName;
				}

				@Override
				public String getComponentType() {
					return "channel";
				}

			});
		}
		return _this();
	}

	/**
	 * The router mapping configuration based on the provided generic key
	 * and  {@link MessageChannel} bean.
	 * The {@link MessageChannel} must be instance of {@link NamedComponent}
	 * for proper target router mapping based on the bean name.
	 * @param key the key.
	 * @param channel the {@link MessageChannel} instance to use.
	 * @return the router spec.
	 * @since 5.2
	 * @see AbstractMappingMessageRouter#setChannelMapping(String, String)
	 */
	public RouterSpec<K, R> channelMapping(K key, final MessageChannel channel) {
		Assert.notNull(key, "'key' must not be null");
		Assert.notNull(channel, "'channel' must not be null");
		Assert.isInstanceOf(NamedComponent.class, channel,
				() -> "The routing channel '" + channel + " must be instance of 'NamedComponent'.");
		this.mappingProvider.addMapping(key, (NamedComponent) channel);
		return _this();
	}

	/**
	 * Add a subflow as an alternative to a {@link #channelMapping(Object, String)}.
	 * {@link #prefix(String)} and {@link #suffix(String)} cannot be used when subflow
	 * mappings are used.
	 * <p> If subflow should refer to the external {@link IntegrationFlow} bean and
	 * there is a requirement to expect reply from there, such a reference should be
	 * wrapped with a {@code .gateway()}:
	 * <pre class="code">
	 * {@code
	 *     .subFlowMapping(false, sf -> sf.gateway(evenFlow())))
	 * }
	 * </pre>
	 * @param key the key.
	 * @param subFlow the subFlow.
	 * @return the router spec.
	 */
	public RouterSpec<K, R> subFlowMapping(K key, IntegrationFlow subFlow) {
		Assert.notNull(key, "'key' must not be null");
		Assert.state(!(StringUtils.hasText(this.prefix) || StringUtils.hasText(this.suffix)),
				"The 'prefix'('suffix') and 'subFlowMapping' are mutually exclusive");

		MessageChannel channel = obtainInputChannelFromFlow(subFlow, false);

		Assert.isInstanceOf(NamedComponent.class, channel,
				() -> "The routing channel '" + channel +
						"' from the flow '" + subFlow + "' must be instance of 'NamedComponent'.");

		this.mappingProvider.addMapping(key, (NamedComponent) channel);
		return _this();
	}

	/**
	 * Make a default output mapping of the router to the parent flow.
	 * Use the next, after router, parent flow {@link MessageChannel} as a
	 * {@link AbstractMessageRouter#setDefaultOutputChannel(MessageChannel)} of this router.
	 * This option also disables {@link AbstractMappingMessageRouter#setChannelKeyFallback(boolean)},
	 * if not called explicitly afterwards, to skip an attempt to resolve the channel name.
	 * @return the router spec.
	 * @since 6.0
	 */
	public RouterSpec<K, R> defaultOutputToParentFlow() {
		return super.defaultOutputToParentFlow()
				.channelKeyFallback(false);
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		// The 'mappingProvider' must be added to the 'componentsToRegister' in the end to
		// let all other components to be registered before the 'RouterMappingProvider.onInit()' logic.
		if (!this.mappingProviderRegistered) {
			if (!this.mappingProvider.mapping.isEmpty()) {
				this.componentsToRegister.put(this.mappingProvider, null);
			}
			this.mappingProviderRegistered = true;
		}
		return super.getComponentsToRegister();
	}

	private static class RouterMappingProvider extends IntegrationObjectSupport {

		private final MappingMessageRouterManagement router;

		private final Map<Object, NamedComponent> mapping = new LinkedHashMap<>();

		RouterMappingProvider(MappingMessageRouterManagement router) {
			this.router = router;
		}

		void addMapping(Object key, NamedComponent channel) {
			this.mapping.put(key, channel);
		}

		@Override
		protected void onInit() {
			super.onInit();
			ConversionService conversionService = getConversionService();
			if (conversionService == null) {
				conversionService = DefaultConversionService.getSharedInstance();
			}
			for (Map.Entry<Object, NamedComponent> entry : this.mapping.entrySet()) {
				Object key = entry.getKey();
				String channelKey;
				if (key instanceof String) {
					channelKey = (String) key;
				}
				else if (key instanceof Class) {
					channelKey = ((Class<?>) key).getName();
				}
				else if (conversionService.canConvert(key.getClass(), String.class)) {
					channelKey = conversionService.convert(key, String.class);
				}
				else {
					throw new MessagingException("Unsupported channel mapping type for router ["
							+ key.getClass() + "]");
				}

				this.router.setChannelMapping(channelKey, entry.getValue().getComponentName());
			}
		}

	}

}
