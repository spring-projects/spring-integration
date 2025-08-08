/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.dsl;

import org.springframework.integration.aggregator.ResequencingMessageGroupProcessor;
import org.springframework.integration.aggregator.ResequencingMessageHandler;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class ResequencerSpec extends CorrelationHandlerSpec<ResequencerSpec, ResequencingMessageHandler> {

	protected ResequencerSpec() {
		super(new ResequencingMessageHandler(new ResequencingMessageGroupProcessor()));
	}

	/**
	 * @param releasePartialSequences the releasePartialSequences
	 * @return the handler spec.
	 * @see ResequencingMessageHandler#setReleasePartialSequences(boolean)
	 */
	public ResequencerSpec releasePartialSequences(boolean releasePartialSequences) {
		this.handler.setReleasePartialSequences(releasePartialSequences);
		return _this();
	}

}
