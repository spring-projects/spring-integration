/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.dsl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandlerSpec} for {@link AbstractMessageRouter}s.
 *
 * @param <S> the target {@link AbstractRouterSpec} implementation type.
 * @param <R> the {@link AbstractMessageRouter} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class AbstractRouterSpec<S extends AbstractRouterSpec<S, R>, R extends AbstractMessageRouter>
		extends MessageHandlerSpec<S, R> implements ComponentsRegistration {

	protected final List<Object> subFlows = new ArrayList<Object>();

	private boolean defaultToParentFlow;

	AbstractRouterSpec(R router) {
		this.target = router;
	}

	/**
	 * @param ignoreSendFailures the ignoreSendFailures.
	 * @return the router spec.
	 * @see AbstractMessageRouter#setIgnoreSendFailures(boolean)
	 */
	public S ignoreSendFailures(boolean ignoreSendFailures) {
		this.target.setIgnoreSendFailures(ignoreSendFailures);
		return _this();
	}

	/**
	 * @param applySequence the applySequence.
	 * @return the router spec.
	 * @see AbstractMessageRouter#setApplySequence(boolean)
	 */
	public S applySequence(boolean applySequence) {
		this.target.setApplySequence(applySequence);
		return _this();
	}

	/**
	 * Specify a {@link MessageChannel} bean name as a default output from the router.
	 * @param channelName the {@link MessageChannel} bean name.
	 * @return the router spec.
	 * @since 1.2
	 * @see AbstractMessageRouter#setDefaultOutputChannelName(String)
	 */
	public S defaultOutputChannel(String channelName) {
		this.target.setDefaultOutputChannelName(channelName);
		return _this();
	}

	/**
	 * Specify a {@link MessageChannel} as a default output from the router.
	 * @param channel the {@link MessageChannel} to use.
	 * @return the router spec.
	 * @since 1.2
	 * @see AbstractMessageRouter#setDefaultOutputChannel(MessageChannel)
	 */
	public S defaultOutputChannel(MessageChannel channel) {
		this.target.setDefaultOutputChannel(channel);
		return _this();
	}

	/**
	 * Specify an {@link IntegrationFlow} as an output from the router when no any other mapping has matched.
	 * @param subFlow the {@link IntegrationFlow} for default mapping.
	 * @return the router spec.
	 * @since 1.2
	 */
	public S defaultSubFlowMapping(IntegrationFlow subFlow) {
		Assert.notNull(subFlow);
		DirectChannel channel = new DirectChannel();
		IntegrationFlowBuilder flowBuilder = IntegrationFlows.from(channel);
		subFlow.configure(flowBuilder);

		this.subFlows.add(flowBuilder);

		return defaultOutputChannel(channel);
	}

	/**
	 * Make a default output mapping of the router to the parent flow.
	 * Use the next, after router, parent flow {@link MessageChannel} as a
	 * {@link AbstractMessageRouter#setDefaultOutputChannel(MessageChannel)} of this router.
	 * @return the router spec.
	 * @since 1.2
	 */
	public S defaultOutputToParentFlow() {
		this.defaultToParentFlow = true;
		return _this();
	}

	boolean isDefaultToParentFlow() {
		return this.defaultToParentFlow;
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		return this.subFlows;
	}

}
