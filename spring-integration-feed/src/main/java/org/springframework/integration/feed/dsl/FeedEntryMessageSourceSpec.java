/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.feed.dsl;

import java.net.URL;

import com.rometools.rome.io.SyndFeedInput;

import org.springframework.core.io.Resource;
import org.springframework.integration.dsl.MessageSourceSpec;
import org.springframework.integration.feed.inbound.FeedEntryMessageSource;
import org.springframework.integration.metadata.MetadataStore;

/**
 * A {@link MessageSourceSpec} for a {@link FeedEntryMessageSource}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class FeedEntryMessageSourceSpec extends MessageSourceSpec<FeedEntryMessageSourceSpec, FeedEntryMessageSource> {

	protected FeedEntryMessageSourceSpec(URL feedUrl, String metadataKey) {
		this.target = new FeedEntryMessageSource(feedUrl, metadataKey);
	}

	protected FeedEntryMessageSourceSpec(Resource feedResource, String metadataKey) {
		this.target = new FeedEntryMessageSource(feedResource, metadataKey);
	}

	public FeedEntryMessageSourceSpec metadataStore(MetadataStore metadataStore) {
		this.target.setMetadataStore(metadataStore);
		return this;
	}

	public FeedEntryMessageSourceSpec syndFeedInput(SyndFeedInput syndFeedInput) {
		this.target.setSyndFeedInput(syndFeedInput);
		return this;
	}

	public FeedEntryMessageSourceSpec preserveWireFeed(boolean preserveWireFeed) {
		this.target.setPreserveWireFeed(preserveWireFeed);
		return this;
	}

}
