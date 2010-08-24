package org.springframework.integration.twitter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.twitter.oauth.OAuthConfiguration;
import org.springframework.test.context.ContextConfiguration;
import twitter4j.Status;
import twitter4j.Twitter;

import java.util.Collection;


/**
 * This class is used to simply demonstrating correctly factory-ing a {@link twitter4j.Twitter} instance
 *
 * @author Josh Long
 */
@ContextConfiguration(locations = "twitter_connection_using_ns.xml")
public class SimpleTwitterTestClient {
	private Twitter twitter;
	@Autowired
	private volatile OAuthConfiguration oAuthConfiguration;

	@Before
	public void begin() throws Exception {
		this.twitter = oAuthConfiguration.getTwitter();
	}

	@Test
	@Ignore
	public void testConnectivity() throws Throwable {
		Collection<Status> responses;
		Assert.assertNotNull(this.twitter);
		Assert.assertNotNull(responses = this.twitter.getFriendsTimeline());
		Assert.assertTrue(responses.size() > 0);
	}
}
