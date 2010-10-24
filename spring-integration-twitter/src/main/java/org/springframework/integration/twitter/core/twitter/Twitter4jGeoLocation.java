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
package org.springframework.integration.twitter.core.twitter;

import org.springframework.integration.twitter.core.GeoLocation;

/**
 * Implements a notion of GeoLocation that forwards calls to {@link twitter4j.GeoLocation} instance
 *
 * @author Josh Long
 * @since 2.0
 */
public class Twitter4jGeoLocation implements GeoLocation {

	private twitter4j.GeoLocation geoLocation;

	public twitter4j.GeoLocation getGeoLocation() {
		return this.geoLocation;
	}
	public Twitter4jGeoLocation(double lat, double lon){
		this.geoLocation = new twitter4j.GeoLocation(lat,lon);
	}
	public Twitter4jGeoLocation(twitter4j.GeoLocation gl) {
		this.geoLocation = gl;
	}

	public double getLongitude() {
		return this.geoLocation.getLongitude();
	}

	public double getLatitude() {
		return this.geoLocation.getLatitude();
	}

}
