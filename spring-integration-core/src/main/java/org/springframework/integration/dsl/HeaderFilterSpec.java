/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.dsl;

import org.springframework.integration.transformer.HeaderFilter;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.util.Assert;

/**
 * A {@link ConsumerEndpointSpec} implementation for the {@link HeaderFilter}.
 *
 * @author Artem Bilan
 *
 * @since 6.2
 */
public class HeaderFilterSpec extends ConsumerEndpointSpec<HeaderFilterSpec, MessageTransformingHandler> {

	private final HeaderFilter headerFilter;

	private final boolean headerFilterExplicitlySet;

	protected HeaderFilterSpec() {
		this(new HeaderFilter(), false);
	}

	protected HeaderFilterSpec(HeaderFilter headerFilter) {
		this(headerFilter, true);
	}

	private HeaderFilterSpec(HeaderFilter headerFilter, boolean headerFilterExplicitlySet) {
		super(new MessageTransformingHandler(headerFilter));
		this.headerFilter = headerFilter;
		this.componentsToRegister.put(this.headerFilter, null);
		this.headerFilterExplicitlySet = headerFilterExplicitlySet;
	}

	public HeaderFilterSpec headersToRemove(String... headersToRemove) {
		assertHeaderFilterNotExplicitlySet();
		this.headerFilter.setHeadersToRemove(headersToRemove);
		return this;
	}

	public HeaderFilterSpec patternMatch(boolean patternMatch) {
		assertHeaderFilterNotExplicitlySet();
		this.headerFilter.setPatternMatch(patternMatch);
		return this;
	}

	private void assertHeaderFilterNotExplicitlySet() {
		Assert.isTrue(!this.headerFilterExplicitlySet,
				() -> "Cannot override already set header filter: " + this.headerFilter);
	}

}
