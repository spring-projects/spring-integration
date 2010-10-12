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
package org.springframework.integration.twitter.oauth;

import twitter4j.http.AccessToken;


/**
 * if the factory bean isn't provided a 'accesstoken' and 'accesstokensecret', then it will try to obtain those bits of informatio on the users behalf.
 * <p/>
 * In doing so it will need input fro the user (automatic or human intervention is required)
 *
 * @author Josh Long
 */
public interface AccessTokenInitialRequestProcessListener {


	String openUrlAndReturnPin(String urlToOpen) throws Exception;

	void persistReturnedAccessToken(AccessToken accessToken) throws Exception;

	void failure(Throwable t);
}
