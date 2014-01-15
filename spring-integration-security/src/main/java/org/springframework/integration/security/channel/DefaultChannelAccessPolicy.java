/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.security.channel;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Creates the {@link ConfigAttribute}s for secured channel
 * send and receive operations based on simple String values.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class DefaultChannelAccessPolicy implements ChannelAccessPolicy {

	private final Collection<ConfigAttribute> configAttributeDefinitionForSend;

	private final Collection<ConfigAttribute> configAttributeDefinitionForReceive;


	/**
	 * Create an access policy instance. The provided 'sendAccess' and 'receiveAccess'
	 * values may be a single String or a comma-delimited list of values. All whitespace
	 * will be trimmed. A <code>null</code> value indicates that the policy does not
	 * apply for either send or receive access type. At most one of the values may be null.
	 *
	 * @param sendAccess The send access value(s).
	 * @param receiveAccess The receive access value(s).
	 */
	@SuppressWarnings("unchecked")
	public DefaultChannelAccessPolicy(String sendAccess, String receiveAccess) {
		boolean sendAccessDefined = StringUtils.hasText(sendAccess);
		boolean receiveAccessDefined = StringUtils.hasText(receiveAccess);
		Assert.isTrue(sendAccessDefined || receiveAccessDefined,
				"At least one of 'sendAccess' and 'receiveAccess' must not be null.");
		if (sendAccessDefined) {
			String[] sendAccessValues = StringUtils.commaDelimitedListToStringArray(sendAccess);
			configAttributeDefinitionForSend = new HashSet<ConfigAttribute>();
			for (String sendAccessValue : sendAccessValues) {
				configAttributeDefinitionForSend.add(new SecurityConfig(StringUtils.trimAllWhitespace(sendAccessValue)));
			}
		}
		else {
			configAttributeDefinitionForSend = Collections.EMPTY_SET;
		}
		if (receiveAccessDefined) {
			String[] receiveAccessValues = StringUtils.commaDelimitedListToStringArray(receiveAccess);
			configAttributeDefinitionForReceive = new HashSet<ConfigAttribute>();
			for (String receiveAccessValue : receiveAccessValues) {
				configAttributeDefinitionForReceive.add(new SecurityConfig(StringUtils.trimAllWhitespace(receiveAccessValue)));
			}
		}
		else {
			configAttributeDefinitionForReceive = Collections.EMPTY_SET;
		}
	}


	@Override
	public Collection<ConfigAttribute> getConfigAttributesForSend() {
		return this.configAttributeDefinitionForSend;
	}

	@Override
	public Collection<ConfigAttribute> getConfigAttributesForReceive() {
		return this.configAttributeDefinitionForReceive;
	}

}
