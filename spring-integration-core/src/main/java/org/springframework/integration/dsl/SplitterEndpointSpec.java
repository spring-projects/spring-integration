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

import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.integration.splitter.DefaultMessageSplitter;
import org.springframework.messaging.MessageChannel;

/**
 * A {@link ConsumerEndpointSpec} for a {@link AbstractMessageSplitter} implementations.
 *
 * @param <S> the target {@link SplitterEndpointSpec} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @deprecated since 6.2 in favor of {@link SplitterSpec}
 */
@Deprecated(since = "6.2", forRemoval = true)
public class SplitterEndpointSpec<S extends AbstractMessageSplitter>
		extends ConsumerEndpointSpec<SplitterEndpointSpec<S>, S> {

	protected SplitterEndpointSpec(S splitter) {
		super(splitter);
	}

	/**
	 * Set the applySequence flag to the specified value. Defaults to {@code true}.
	 * @param applySequence the applySequence.
	 * @return the endpoint spec.
	 * @see AbstractMessageSplitter#setApplySequence(boolean)
	 */
	public SplitterEndpointSpec<S> applySequence(boolean applySequence) {
		this.handler.setApplySequence(applySequence);
		return this;
	}

	/**
	 * Set delimiters to tokenize String values. The default is
	 * <code>null</code> indicating that no tokenizing should occur.
	 * If delimiters are provided, they will be applied to any String payload.
	 * Only applied if provided {@code splitter} is instance of {@link DefaultMessageSplitter}.
	 * @param delimiters The delimiters.
	 * @return the endpoint spec.
	 * @see DefaultMessageSplitter#setDelimiters(String)
	 */
	public SplitterEndpointSpec<S> delimiters(String delimiters) {
		if (this.handler instanceof DefaultMessageSplitter) {
			((DefaultMessageSplitter) this.handler).setDelimiters(delimiters);
		}
		else {
			logger.warn("'delimiters' can be applied only for the DefaultMessageSplitter");
		}
		return this;
	}

	/**
	 * Specify a channel where rejected Messages should be sent. If the discard
	 * channel is null (the default), rejected Messages will be dropped.
	 * A "Rejected Message" means that split function has returned an empty result (but not null):
	 * no items to iterate for sending.
	 * @param discardChannel The discard channel.
	 * @return the endpoint spec.
	 * @since 5.2
	 * @see DefaultMessageSplitter#setDelimiters(String)
	 */
	public SplitterEndpointSpec<S> discardChannel(MessageChannel discardChannel) {
		this.handler.setDiscardChannel(discardChannel);
		return this;
	}

	/**
	 * Specify a channel bean name where rejected Messages should be sent. If the discard
	 * channel is null (the default), rejected Messages will be dropped.
	 * A "Rejected Message" means that split function has returned an empty result (but not null):
	 * no items to iterate for sending.
	 * @param discardChannelName The discard channel bean name.
	 * @return the endpoint spec.
	 * @since 5.2
	 * @see DefaultMessageSplitter#setDelimiters(String)
	 */
	public SplitterEndpointSpec<S> discardChannel(String discardChannelName) {
		this.handler.setDiscardChannelName(discardChannelName);
		return this;
	}

	/**
	 * Configure a subflow to run for discarded messages instead of a
	 * {@link #discardChannel(MessageChannel)}.
	 * @param discardFlow the discard flow.
	 * @return the endpoint spec.
	 * @since 5.2
	 */
	public SplitterEndpointSpec<S> discardFlow(IntegrationFlow discardFlow) {
		return discardChannel(obtainInputChannelFromFlow(discardFlow));
	}

}
