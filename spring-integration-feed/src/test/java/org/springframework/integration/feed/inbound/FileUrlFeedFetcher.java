/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.feed.inbound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import org.springframework.util.Assert;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FetcherEvent;
import com.sun.syndication.fetcher.FetcherException;
import com.sun.syndication.fetcher.impl.AbstractFeedFetcher;
import com.sun.syndication.fetcher.impl.SyndFeedInfo;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
class FileUrlFeedFetcher extends AbstractFeedFetcher {

	/**
	 * Retrieve a SyndFeed for the given URL.
	 * @see com.sun.syndication.fetcher.FeedFetcher#retrieveFeed(java.net.URL)
	 */
	public SyndFeed retrieveFeed(URL feedUrl) throws IOException, FeedException, FetcherException {
		Assert.notNull(feedUrl, "feedUrl must not be null");
		URLConnection connection = feedUrl.openConnection();
		SyndFeedInfo syndFeedInfo = new SyndFeedInfo();
		this.refreshFeedInfo(feedUrl, syndFeedInfo, connection);
		return syndFeedInfo.getSyndFeed();
	}

	private void refreshFeedInfo(URL feedUrl, SyndFeedInfo syndFeedInfo, URLConnection connection) throws IOException, FeedException {
		// need to always set the URL because this may have changed due to 3xx redirects
		syndFeedInfo.setUrl(connection.getURL());

		// the ID is a persistent value that should stay the same
		// even if the URL for the feed changes (eg, by 3xx redirects)
		syndFeedInfo.setId(feedUrl.toString());

		// This will be 0 if the server doesn't support or isn't setting the last modified header
		syndFeedInfo.setLastModified(new Long(connection.getLastModified()));

		// get the contents
		InputStream inputStream = null;
		try {
			inputStream = connection.getInputStream();
			SyndFeed syndFeed = this.readFeedFromStream(inputStream, connection);
			syndFeedInfo.setSyndFeed(syndFeed);
		}
		finally {
			try {
				inputStream.close();
			}
			catch (Exception e) {
				// ignore
			}
		}
	}

	private SyndFeed readFeedFromStream(InputStream inputStream, URLConnection connection) throws IOException, FeedException {
		BufferedInputStream bufferedInputStream;
		if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) {
			// handle gzip encoded content
			bufferedInputStream = new BufferedInputStream(new GZIPInputStream(inputStream));
		}
		else {
			bufferedInputStream = new BufferedInputStream(inputStream);
		}
		XmlReader reader = null;
		if (connection.getHeaderField("Content-Type") != null) {
			reader = new XmlReader(bufferedInputStream, connection.getHeaderField("Content-Type"), true);
		}
		else {
			reader = new XmlReader(bufferedInputStream, true);
		}
		SyndFeedInput syndFeedInput = new SyndFeedInput();
		syndFeedInput.setPreserveWireFeed(isPreserveWireFeed());
		SyndFeed feed = syndFeedInput.build(reader);
		fireEvent(FetcherEvent.EVENT_TYPE_FEED_RETRIEVED, connection, feed);
		return feed;
	}

}
