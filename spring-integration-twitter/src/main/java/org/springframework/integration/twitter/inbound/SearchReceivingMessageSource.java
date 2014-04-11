/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.twitter.inbound;

import java.util.Collections;
import java.util.List;

import org.springframework.social.twitter.api.SearchParameters;
import org.springframework.social.twitter.api.SearchResults;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.util.Assert;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Gary Russell
 * @since 2.0
 */
public class SearchReceivingMessageSource extends AbstractTwitterMessageSource<Tweet> {

	private volatile String query;


	public SearchReceivingMessageSource(Twitter twitter, String metadataKey) {
		super(twitter, metadataKey);
	}

	public void setQuery(String query) {
		Assert.hasText(query, "'query' must not be null");
		this.query = query;
	}

	@Override
	public String getComponentType() {
		return "twitter:search-inbound-channel-adapter";
	}

	@Override
	protected List<Tweet> pollForTweets(long sinceId) {
		SearchParameters searchParameters = new SearchParameters(query).count(this.getPageSize()).sinceId(sinceId);
		SearchResults results = this.getTwitter().searchOperations().search(searchParameters);
		return (results != null) ? results.getTweets() : Collections.<Tweet>emptyList();
	}

}
