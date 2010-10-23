package org.springframework.integration.twitter.model;

/**
 * Implements a notion of GeoLocation that forwards calls to {@link twitter4j.GeoLocation} instance
 *
 * @author Josh Long
 */
public class Twitter4jGeoLocationImpl implements GeoLocation {

	private twitter4j.GeoLocation geoLocation;

	public twitter4j.GeoLocation getGeoLocation() {
		return this.geoLocation;
	}

	public Twitter4jGeoLocationImpl(twitter4j.GeoLocation gl) {
		this.geoLocation = gl;
	}

	public double getLongitude() {
		return this.geoLocation.getLongitude();
	}

	public double getLatitude() {
		return this.geoLocation.getLatitude();
	}

}
