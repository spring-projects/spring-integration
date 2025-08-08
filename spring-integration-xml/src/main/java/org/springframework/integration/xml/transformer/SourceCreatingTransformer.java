/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
