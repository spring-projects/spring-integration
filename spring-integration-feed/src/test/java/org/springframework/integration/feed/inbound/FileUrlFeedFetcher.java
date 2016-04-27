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

package org.springframework.integration.feed.inbound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import org.springframework.util.Assert;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.0
 * @deprecated since 4.3 because 'rome-fetcher-1.6.0' is deprecated.
 * Will be revised in 5.0 in favor of ROME 2.0
 *
 */
@SuppressWarnings("deprecation")
@Deprecated
class FileUrlFeedFetcher extends com.rometools.fetcher.impl.AbstractFeedFetcher {

	@Override
	public SyndFeed retrieveFeed(URL feedUrl)
			throws IOException, FeedException, com.rometools.fetcher.FetcherException {
		Assert.notNull(feedUrl, "feedUrl must not be null");
		URLConnection connection = feedUrl.openConnection();
		com.rometools.fetcher.impl.SyndFeedInfo syndFeedInfo = new com.rometools.fetcher.impl.SyndFeedInfo();
		this.refreshFeedInfo(feedUrl, syndFeedInfo, connection);
		return syndFeedInfo.getSyndFeed();
	}

	@Override
	public SyndFeed retrieveFeed(String userAgent, URL url)
			throws IllegalArgumentException, IOException, FeedException, com.rometools.fetcher.FetcherException {
		return retrieveFeed(url);
	}

	private void refreshFeedInfo(URL feedUrl, com.rometools.fetcher.impl.SyndFeedInfo syndFeedInfo,
			URLConnection connection)
			throws IOException, FeedException {
		// need to always set the URL because this may have changed due to 3xx redirects
		syndFeedInfo.setUrl(connection.getURL());

		// the ID is a persistent value that should stay the same
		// even if the URL for the feed changes (eg, by 3xx redirects)
		syndFeedInfo.setId(feedUrl.toString());

		// This will be 0 if the server doesn't support or isn't setting the last modified header
		syndFeedInfo.setLastModified(connection.getLastModified());

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

	private SyndFeed readFeedFromStream(InputStream inputStream, URLConnection connection)
			throws IOException, FeedException {
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
		fireEvent(com.rometools.fetcher.FetcherEvent.EVENT_TYPE_FEED_RETRIEVED, connection, feed);
		return feed;
	}

}
