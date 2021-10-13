/*
 * Copyright 2016-2021 the original author or authors.
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

import java.util.Map;
import java.util.function.Function;

import org.springframework.integration.aggregator.AbstractAggregatingMessageGroupProcessor;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor;
import org.springframework.integration.aggregator.DelegatingMessageGroupProcessor;
import org.springframework.integration.aggregator.ExpressionEvaluatingMessageGroupProcessor;
import org.springframework.integration.aggregator.MessageGroupProcessor;
import org.springframework.integration.aggregator.MethodInvokingMessageGroupProcessor;
import org.springframework.integration.store.MessageGroup;

/**
 * A {@link CorrelationHandlerSpec} for an {@link AggregatingMessageHandler}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class AggregatorSpec extends CorrelationHandlerSpec<AggregatorSpec, AggregatingMessageHandler> {

	private Function<MessageGroup, Map<String, Object>> headersFunction;

	protected AggregatorSpec() {
		super(new AggregatingMessageHandler(new DefaultAggregatingMessageGroupProcessor()));
	}

	/**
	 * Configure the handler with {@link org.springframework.integration.aggregator.MethodInvokingCorrelationStrategy}
	 * and {@link org.springframework.integration.aggregator.MethodInvokingReleaseStrategy} using the target
	 * object which should have methods annotated appropriately for each function.
	 * Also set the output processor.
	 * @param target the target object.
	 * @return the handler spec.
	 */
	public AggregatorSpec processor(Object target) {
		return processor(target, null);
	}

	/**
	 * Configure the handler with {@link org.springframework.integration.aggregator.MethodInvokingCorrelationStrategy}
	 * and {@link org.springframework.integration.aggregator.MethodInvokingReleaseStrategy} using the target
	 * object which should have methods annotated appropriately for each function.
	 * Also set the output processor.
	 * @param target the target object.
	 * @param methodName The method name for the output processor (or 'null' in which case, the
	 * target object must have an {@link org.springframework.integration.annotation.Aggregator} annotation).
	 * @return the handler spec.
	 */
	public AggregatorSpec processor(Object target, String methodName) {
		return super.processor(target)
				.outputProcessor(methodName != null
						? new MethodInvokingMessageGroupProcessor(target, methodName)
						:
						(target instanceof MessageGroupProcessor
								? (MessageGroupProcessor) target
								: new MethodInvokingMessageGroupProcessor(target)));
	}

	/**
	 * An expression to determine the output message from the released group. Defaults to a message
	 * with a payload that is a collection of payloads from the input messages.
	 * @param expression the expression.
	 * @return the aggregator spec.
	 */
	public AggregatorSpec outputExpression(String expression) {
		return outputProcessor(new ExpressionEvaluatingMessageGroupProcessor(expression));
	}

	/**
	 * A processor to determine the output message from the released group. Defaults to a message
	 * with a payload that is a collection of payloads from the input messages.
	 * @param outputProcessor the processor.
	 * @return the aggregator spec.
	 */
	public AggregatorSpec outputProcessor(MessageGroupProcessor outputProcessor) {
		this.handler.setOutputProcessor(outputProcessor);
		return _this();
	}

	/**
	 * @param expireGroupsUponCompletion the expireGroupsUponCompletion.
	 * @return the aggregator spec.
	 * @see AggregatingMessageHandler#setExpireGroupsUponCompletion(boolean)
	 */
	public AggregatorSpec expireGroupsUponCompletion(boolean expireGroupsUponCompletion) {
		this.handler.setExpireGroupsUponCompletion(expireGroupsUponCompletion);
		return _this();
	}

	/**
	 * Configure a {@link Function} to merge and compute headers for reply
	 * based on the completed {@link MessageGroup}.
	 * @param headersFunction the {@link Function} to merge and compute headers for reply
	 * based on the completed {@link MessageGroup}.
	 * @return the aggregator spec.
	 * @since 5.2
	 */
	public AggregatorSpec headersFunction(Function<MessageGroup, Map<String, Object>> headersFunction) {
		this.headersFunction = headersFunction;
		return _this();
	}


	@Override
	public Map<Object, String> getComponentsToRegister() {
		if (this.headersFunction != null) {
			MessageGroupProcessor outputProcessor = this.handler.getOutputProcessor();
			if (outputProcessor instanceof AbstractAggregatingMessageGroupProcessor) {
				((AbstractAggregatingMessageGroupProcessor) outputProcessor).setHeadersFunction(this.headersFunction);
			}
			else {
				this.handler.setOutputProcessor(
						new DelegatingMessageGroupProcessor(outputProcessor, this.headersFunction));
			}
		}
		return super.getComponentsToRegister();
	}

}
