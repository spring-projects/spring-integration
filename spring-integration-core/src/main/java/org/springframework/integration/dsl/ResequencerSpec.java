/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
