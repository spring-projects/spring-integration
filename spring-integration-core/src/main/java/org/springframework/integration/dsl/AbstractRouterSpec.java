/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
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
