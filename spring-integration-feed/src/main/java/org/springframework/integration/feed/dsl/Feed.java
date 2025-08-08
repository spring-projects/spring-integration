/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.feed.dsl;

import java.net.URL;

import org.springframework.core.io.Resource;

/**
 * The Spring Integration Feed components Factory.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public final class Feed {

	public static FeedEntryMessageSourceSpec inboundAdapter(URL feedUrl, String metadataKey) {
		return new FeedEntryMessageSourceSpec(feedUrl, metadataKey);
	}

	public static FeedEntryMessageSourceSpec inboundAdapter(Resource feedResource, String metadataKey) {
		return new FeedEntryMessageSourceSpec(feedResource, metadataKey);
	}

	private Feed() {
	}

}
