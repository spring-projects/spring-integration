/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.integration.aot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ProxyHints;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.SerializationHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.DecoratingProxy;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.core.Pausable;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.gateway.MethodArgsHolder;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.DelayHandler;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.json.JsonPathUtils;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.routingslip.ExpressionEvaluatingRoutingSlipRouteStrategy;
import org.springframework.integration.store.MessageGroupMetadata;
import org.springframework.integration.store.MessageHolder;
import org.springframework.integration.store.MessageMetadata;
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.integration.support.management.ManageableSmartLifecycle;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

/**
 * {@link RuntimeHintsRegistrar} for Spring Integration core module.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
class CoreRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		ReflectionHints reflectionHints = hints.reflection();
		Stream.of(
						GenericSelector.class,
						GenericTransformer.class,
						GenericHandler.class,
						Function.class,
						Supplier.class,
						BeanExpressionContext.class,
						IntegrationContextUtils.class,
						MethodArgsHolder.class,
						AbstractReplyProducingMessageHandler.RequestHandler.class,
						ExpressionEvaluatingRoutingSlipRouteStrategy.RequestAndReply.class,
						Pausable.class,
						ManageableSmartLifecycle.class)
				.forEach(type ->
						reflectionHints.registerType(type,
								builder -> builder.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS)));

		reflectionHints.registerType(JsonPathUtils.class,
				builder ->
						builder.onReachableType(TypeReference.of("com.jayway.jsonpath.JsonPath"))
								.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));

		// For #xpath() SpEL function
		reflectionHints.registerTypeIfPresent(classLoader, "org.springframework.integration.xml.xpath.XPathUtils",
				builder -> builder.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));

		Stream.of(
						"kotlin.jvm.functions.Function0",
						"kotlin.jvm.functions.Function1",
						"kotlin.Unit")
				.forEach(type ->
						reflectionHints.registerTypeIfPresent(classLoader, type,
								builder -> builder.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS)));

		hints.resources().registerPattern("META-INF/spring.integration.properties");

		SerializationHints serializationHints = hints.serialization();
		Stream.of(
						String.class,
						Number.class,
						Long.class,
						Date.class,
						ArrayList.class,
						HashMap.class,
						Properties.class,
						Hashtable.class,
						Exception.class,
						UUID.class,
						GenericMessage.class,
						ErrorMessage.class,
						MessageHeaders.class,
						AdviceMessage.class,
						MutableMessage.class,
						MutableMessageHeaders.class,
						MessageGroupMetadata.class,
						MessageHolder.class,
						MessageMetadata.class,
						MessageHistory.class,
						MessageHistory.Entry.class,
						DelayHandler.DelayedMessageWrapper.class)
				.forEach(serializationHints::registerType);

		ProxyHints proxyHints = hints.proxies();

		registerSpringJdkProxy(proxyHints, AbstractReplyProducingMessageHandler.RequestHandler.class);
		registerSpringJdkProxy(proxyHints, IntegrationFlow.class, SmartLifecycle.class);
	}

	private static void registerSpringJdkProxy(ProxyHints proxyHints, Class<?>... proxiedInterfaces) {
		proxyHints
				.registerJdkProxy(builder ->
						builder.proxiedInterfaces(proxiedInterfaces)
								.proxiedInterfaces(SpringProxy.class, Advised.class, DecoratingProxy.class));
	}

}
