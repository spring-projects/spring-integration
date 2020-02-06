/*
 * Copyright 2016-2020 the original author or authors.
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

import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.messaging.MessageChannel;

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
		extends ConsumerEndpointSpec<S, R> {

	private boolean defaultToParentFlow;

	protected AbstractRouterSpec(R router) {
		super(router);
	}

	/**
	 * @param ignoreSendFailures the ignoreSendFailures.
	 * @return the router spec.
	 * @see AbstractMessageRouter#setIgnoreSendFailures(boolean)
	 */
	public S ignoreSendFailures(boolean ignoreSendFailures) {
		this.handler.setIgnoreSendFailures(ignoreSendFailures);
		return _this();
	}

	/**
	 * @param applySequence the applySequence.
	 * @return the router spec.
	 * @see AbstractMessageRouter#setApplySequence(boolean)
	 */
	public S applySequence(boolean applySequence) {
		this.handler.setApplySequence(applySequence);
		return _this();
	}

	/**
	 * Specify a {@link MessageChannel} bean name as a default output from the router.
	 * @param channelName the {@link MessageChannel} bean name.
	 * @return the router spec.
	 * @see AbstractMessageRouter#setDefaultOutputChannelName(String)
	 */
	public S defaultOutputChannel(String channelName) {
		this.handler.setDefaultOutputChannelName(channelName);
		return _this();
	}

	/**
	 * Specify a {@link MessageChannel} as a default output from the router.
	 * @param channel the {@link MessageChannel} to use.
	 * @return the router spec.
	 * @see AbstractMessageRouter#setDefaultOutputChannel(MessageChannel)
	 */
	public S defaultOutputChannel(MessageChannel channel) {
		this.handler.setDefaultOutputChannel(channel);
		return _this();
	}

	/**
	 * Specify an {@link IntegrationFlow} as an output from the router when no any other mapping has matched.
	 * @param subFlow the {@link IntegrationFlow} for default mapping.
	 * @return the router spec.
	 */
	public S defaultSubFlowMapping(IntegrationFlow subFlow) {
		return defaultOutputChannel(obtainInputChannelFromFlow(subFlow, false));
	}

	/**
	 * Make a default output mapping of the router to the parent flow.
	 * Use the next, after router, parent flow {@link MessageChannel} as a
	 * {@link AbstractMessageRouter#setDefaultOutputChannel(MessageChannel)} of this router.
	 * @return the router spec.
	 */
	public S defaultOutputToParentFlow() {
		this.defaultToParentFlow = true;
		return _this();
	}

	protected boolean isDefaultToParentFlow() {
		return this.defaultToParentFlow;
	}

}
