/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.xml.transformer;

import javax.xml.transform.Source;

import org.springframework.integration.transformer.AbstractPayloadTransformer;
import org.springframework.integration.xml.source.DomSourceFactory;
import org.springframework.integration.xml.source.SourceFactory;

/**
 * Transforms the payload to a {@link Source} using a {@link SourceFactory}.
 * Defaults to using a {@link DomSourceFactory} if an alternative is not provided.
 *
 * @author Jonas Partner
 */
public class SourceCreatingTransformer extends AbstractPayloadTransformer<Object, Source> {

	private final SourceFactory sourceFactory;

	public SourceCreatingTransformer() {
		this.sourceFactory = new DomSourceFactory();
	}

	public SourceCreatingTransformer(SourceFactory sourceFactory) {
		this.sourceFactory = sourceFactory;
	}

	@Override
	public Source transformPayload(Object payload) {
		return this.sourceFactory.createSource(payload);
	}

}
