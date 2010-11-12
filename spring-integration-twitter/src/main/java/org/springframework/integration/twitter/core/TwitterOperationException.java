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

import org.springframework.util.StringUtils;

import twitter4j.TwitterException;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
@SuppressWarnings("serial")
public class TwitterOperationException extends RuntimeException {

	private int twitterStatusCode = -1;


	public TwitterOperationException() {
		this(null, null);
	}

	/**
	 * @param description
	 */
	public TwitterOperationException(String description) {
		this(description, null);
	}

	/**
	 * @param throwable
	 */
	public TwitterOperationException(Throwable throwable) {
		this(null, throwable);
	}

	/**
	 * @param description
	 * @param throwable
	 */
	public TwitterOperationException(String description, Throwable throwable) {
		super(formatDescription(description, throwable), throwable);
		if (throwable instanceof TwitterException){
			this.twitterStatusCode = ((TwitterException)throwable).getStatusCode();
		}
	}


	public int getTwitterStatusCode() {
		return twitterStatusCode;
	}


	private static String formatDescription(String description, Throwable throwable) {
		StringBuffer buffer = new StringBuffer();
		if (StringUtils.hasText(description)){
			buffer.append(description + " ");
		}
		if (throwable != null && throwable instanceof TwitterException){
			TwitterException te = (TwitterException) throwable;
			String message = te.getMessage();
			if (StringUtils.hasText(message)) {
				buffer.append("Detailed Error: ");
				if (message.contains("{")){
					buffer.append(message.substring(0, message.indexOf("{")));
				}
				else {
					buffer.append(message);
				}
			}	
			buffer.append("For more information about this exception please visit the following Twitter website: " +
					"http://apiwiki.twitter.com/w/page/22554652/HTTP-Response-Codes-and-Errors");
		}
		return buffer.toString();
	}

}
