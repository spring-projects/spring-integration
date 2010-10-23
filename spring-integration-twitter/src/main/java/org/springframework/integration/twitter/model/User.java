package org.springframework.integration.twitter.model;


/**
 * describes the user producing or receiving messages
 *
 * @author Josh Long
 */
public interface User {

    int getId();

    java.lang.String getName();

    java.lang.String getScreenName();

    java.lang.String getLocation();

    java.lang.String getDescription();

    boolean isContributorsEnabled();

    java.net.URL getProfileImageURL();

    java.net.URL getURL();

    boolean isProtected();

    int getFollowersCount();
}
