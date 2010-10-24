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
package org.springframework.integration.twitter.core;


/**
 * An enum to allow users to express interest in particular kinds of tweets.
 * <p/>
 * Contains header keys used by the various adapters.
 *
 * @author Josh Long
 * @since 2.0
 */
public class TwitterHeaders {
	public static final String TWITTER_IN_REPLY_TO_STATUS_ID = "TWITTER_IN_REPLY_TO_STATUS_ID";
	public static final String TWITTER_PLACE_ID = "TWITTER_PLACE_ID";
	public static final String TWITTER_GEOLOCATION = "TWITTER_GEOLOCATION";
	public static final String TWITTER_DISPLAY_COORDINATES = "TWITTER_DISPLAY_COORDINATES";
	public static final String TWITTER_DM_TARGET_USER_ID = "TWITTER_DM_TARGET_USER_ID";
}
