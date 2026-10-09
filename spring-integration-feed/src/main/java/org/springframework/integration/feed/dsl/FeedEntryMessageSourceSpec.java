/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.feed.dsl;

import java.net.URL;
import java.util.Date;
import java.util.function.BiFunction;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;

import org.springframework.core.io.Resource;
import org.springframework.integration.dsl.MessageSourceSpec;
import org.springframework.integration.feed.inbound.FeedEntryMessageSource;
import org.springframework.integration.metadata.MetadataStore;

/**
 * A {@link MessageSourceSpec} for a {@link FeedEntryMessageSource}.
 *
 * @author Artem Bilan
 * @author Jialin Chen
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

	/**
	 * Specify a function to determine a non-null date for each entry in a feed.
	 * @param entryDateFunction the function, which must return a non-null date.
	 * @return the spec.
	 * @since 7.2
	 * @see FeedEntryMessageSource#setEntryDateFunction(BiFunction)
	 */
	public FeedEntryMessageSourceSpec entryDateFunction(
			BiFunction<SyndEntry, SyndFeed, Date> entryDateFunction) {

		this.target.setEntryDateFunction(entryDateFunction);
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
