/*
* Copyright 2010 the original author or authors
*
*     Licensed under the Apache License, Version 2.0 (the "License");
*     you may not use this file except in compliance with the License.
*     You may obtain a copy of the License at
*
*         http://www.apache.org/licenses/LICENSE-2.0
*
*     Unless required by applicable law or agreed to in writing, software
*     distributed under the License is distributed on an "AS IS" BASIS,
*     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*     See the License for the specific language governing permissions and
*     limitations under the License.
*/
package org.springframework.integration.feed;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

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
 * @since 2.0
 */
public class FileUrlFeedFetcher extends AbstractFeedFetcher {

	/* (non-Javadoc)
	 * @see com.sun.syndication.fetcher.FeedFetcher#retrieveFeed(java.net.URL)
	 */
	public SyndFeed retrieveFeed(URL feedUrl) throws IllegalArgumentException,
			IOException, FeedException, FetcherException {
		if (feedUrl == null) {
			throw new IllegalArgumentException("null is not a valid URL");
		}
		
		URLConnection connection = feedUrl.openConnection();

		SyndFeedInfo syndFeedInfo = new SyndFeedInfo();
		retrieveAndCacheFeed(feedUrl, syndFeedInfo, connection);
		return syndFeedInfo.getSyndFeed();
	}

	protected void retrieveAndCacheFeed(URL feedUrl, SyndFeedInfo syndFeedInfo, URLConnection connection) throws IllegalArgumentException, FeedException, FetcherException, IOException {
		resetFeedInfo(feedUrl, syndFeedInfo, connection);
	}
	
	protected void resetFeedInfo(URL orignalUrl, SyndFeedInfo syndFeedInfo, URLConnection connection) throws IllegalArgumentException, IOException, FeedException {
		// need to always set the URL because this may have changed due to 3xx redirects
		syndFeedInfo.setUrl(connection.getURL());

		// the ID is a persistant value that should stay the same even if the URL for the
		// feed changes (eg, by 3xx redirects)
		syndFeedInfo.setId(orignalUrl.toString());

		// This will be 0 if the server doesn't support or isn't setting the last modified header
		syndFeedInfo.setLastModified(new Long(connection.getLastModified()));

		// get the contents
		InputStream inputStream = null;
		try {
			inputStream = connection.getInputStream();
			SyndFeed syndFeed = getSyndFeedFromStream(inputStream, connection);		
			syndFeedInfo.setSyndFeed(syndFeed);
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}
	private SyndFeed getSyndFeedFromStream(InputStream inputStream, URLConnection connection) throws IOException, IllegalArgumentException, FeedException {
		SyndFeed feed = readSyndFeedFromStream(inputStream, connection);
		fireEvent(FetcherEvent.EVENT_TYPE_FEED_RETRIEVED, connection, feed);
		return feed;
	}
	private SyndFeed readSyndFeedFromStream(InputStream inputStream, URLConnection connection) throws IOException, IllegalArgumentException, FeedException {
		BufferedInputStream is;
		if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) {
			// handle gzip encoded content
			is = new BufferedInputStream(new GZIPInputStream(inputStream));
		} else {
			is = new BufferedInputStream(inputStream);
		}

	    XmlReader reader = null;	    
	    if (connection.getHeaderField("Content-Type") != null) {
	        reader = new XmlReader(is, connection.getHeaderField("Content-Type"), true);
	    } else {
	        reader = new XmlReader(is, true);
	    }
	    
	    SyndFeedInput syndFeedInput = new SyndFeedInput();
	    syndFeedInput.setPreserveWireFeed(isPreserveWireFeed());
	    
		return syndFeedInput.build(reader); 
	}
}
