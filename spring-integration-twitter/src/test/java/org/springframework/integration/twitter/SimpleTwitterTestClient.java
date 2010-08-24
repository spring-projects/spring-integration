package org.springframework.integration.twitter;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.integration.twitter.oauth.OAuthConfiguration;

import org.springframework.test.context.ContextConfiguration;

import twitter4j.Status;
import twitter4j.Twitter;

import java.util.Collection;

import javax.annotation.PostConstruct;


/**
 * This class is used to simply demonstrating correctly factory-ing a {@link twitter4j.Twitter} instance
 */
@ContextConfiguration(locations = "twitter_connection_using_ns.xml")
public class SimpleTwitterTestClient {
    private Twitter twitter;
    @Autowired
    private volatile OAuthConfiguration oAuthConfiguration;

    @PostConstruct
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
