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

import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.integration.splitter.DefaultMessageSplitter;

/**
 * A {@link ConsumerEndpointSpec} for a {@link AbstractMessageSplitter} implementations.
 *
 * @param <S> the target {@link SplitterEndpointSpec} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public final class SplitterEndpointSpec<S extends AbstractMessageSplitter>
		extends ConsumerEndpointSpec<SplitterEndpointSpec<S>, S> {

	SplitterEndpointSpec(S splitter) {
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
		return _this();
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

}
