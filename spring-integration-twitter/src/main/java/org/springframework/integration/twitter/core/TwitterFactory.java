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
package org.springframework.integration.twitter.core;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.util.Assert;

import twitter4j.DirectMessage;
import twitter4j.Status;

/**
 * Factory class with several static factory methods for constructing 
 * Twitter objects that are independent from the underlying API (e.g., twitter4j)
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class TwitterFactory {

	private TwitterFactory(){}
	
	/**
	 * Will construct either {@link org.springframework.integration.twitter.core.Status} or
	 * {@link org.springframework.integration.twitter.core.DirectMessage} from the instances of 
	 * {@link Status} pr {@link DirectMessage}
	 * 
	 * @param twitterMessage
	 * @return
	 */
	public static TwitterMessage formTwitter4jMessage(Object twitterMessage){
		Assert.isTrue(twitterMessage instanceof Status || twitterMessage instanceof DirectMessage, 
				"'twitterMessage' must be an instance of either twitter4j.DirectMessage or twitter4j.Status");
		Class<?> interfaze = twitterMessage instanceof DirectMessage 
			? org.springframework.integration.twitter.core.DirectMessage.class
			: org.springframework.integration.twitter.core.Status.class;
		ProxyFactory factory = new ProxyFactory(interfaze, EmptyTargetSource.INSTANCE);
		factory.addAdvice(new Twitter4jDecorator(twitterMessage));
		return (TwitterMessage) factory.getProxy();
	}
	/**
	 * Will constuct {@link GeoLocation} from 'latitude' and 'longitude'
	 * 
	 * @param latitude
	 * @param longitude
	 * @return
	 */
	public static GeoLocation fromLatitudeLongitude(double latitude, double longitude){
		twitter4j.GeoLocation geolocation = new twitter4j.GeoLocation(latitude, longitude);
		return fromTwitter4jGeoLocation(geolocation);
	}
	/**
	 * Will constuct {@link GeoLocation} from {@link twitter4j.GeoLocation}
	 * @param geolocation
	 * @return
	 */
	public static GeoLocation fromTwitter4jGeoLocation(twitter4j.GeoLocation geolocation){
		ProxyFactory factory = new ProxyFactory(GeoLocation.class, EmptyTargetSource.INSTANCE);
		factory.addAdvice(new Twitter4jDecorator(geolocation));
		return (GeoLocation) factory.getProxy();
	}
}
