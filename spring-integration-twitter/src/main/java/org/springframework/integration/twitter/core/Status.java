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

import java.util.Date;

/**
 * describes a simple status message on twitter. this could be a message
 * that updates a timeline on behalf of a user, or it could be a message that contains a reply.
 *
 *
 * @author Josh Long
 * @since 2.0 
 */
public interface Status {

 	Date getCreatedAt();

	long getId();

	String getText();

	String getSource();

	boolean isTruncated();

	long getInReplyToStatusId();

	int getInReplyToUserId();

	java.lang.String getInReplyToScreenName();

	boolean isFavorited();

	 User getUser();

	boolean isRetweet();

	 Status getRetweetedStatus();

	String[] getContributors();

}
