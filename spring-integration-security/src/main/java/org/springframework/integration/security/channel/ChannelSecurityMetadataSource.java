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

package org.springframework.integration.security.channel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.MessageChannel;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityMetadataSource;
import org.springframework.util.Assert;

/**
 * The {@link SecurityMetadataSource} implementation for secured {@link MessageChannel}s.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class ChannelSecurityMetadataSource implements SecurityMetadataSource {

	private final Map<Pattern, ChannelAccessPolicy> patternMappings;


	public ChannelSecurityMetadataSource() {
		this(null);
	}

	public ChannelSecurityMetadataSource(Map<Pattern, ChannelAccessPolicy> patternMappings) {
		this.patternMappings = (patternMappings != null) ? patternMappings
				: new LinkedHashMap<Pattern, ChannelAccessPolicy>();
	}


	public void addPatternMapping(Pattern pattern, ChannelAccessPolicy accessPolicy) {
		this.patternMappings.put(pattern, accessPolicy);
	}

	public Set<Pattern> getPatterns() {
		return this.patternMappings.keySet();
	}

	public Collection<ConfigAttribute> getAttributes(Object object) throws IllegalArgumentException {
		Assert.isAssignable(ChannelInvocation.class, object.getClass());
		ChannelInvocation invocation = (ChannelInvocation) object;
		MessageChannel channel = invocation.getChannel();
		Assert.isAssignable(NamedComponent.class, channel.getClass());
		String channelName = ((NamedComponent) channel).getComponentName();
		List<ConfigAttribute> attributes = new ArrayList<ConfigAttribute>();
		for (Map.Entry<Pattern, ChannelAccessPolicy> mapping : this.patternMappings.entrySet()) {
			Pattern pattern = mapping.getKey();
			ChannelAccessPolicy accessPolicy = mapping.getValue();
			if (pattern.matcher(channelName).matches()) {
				if (invocation.isSend()) {
					Collection<ConfigAttribute> definition = accessPolicy.getConfigAttributesForSend();
					if (definition != null) {
						attributes.addAll(definition);
					}
				}
				else if (invocation.isReceive()) {
					Collection<ConfigAttribute> definition = accessPolicy.getConfigAttributesForReceive();
					if (definition != null) {
						attributes.addAll(definition);
					}
				}
			}
		}
		return attributes;
	}

	public Collection<ConfigAttribute> getAllConfigAttributes() {
		Set<ConfigAttribute> allAttributes = new HashSet<ConfigAttribute>();
        for (ChannelAccessPolicy policy : patternMappings.values()) {
        	Collection<ConfigAttribute> receiveAttributes = policy.getConfigAttributesForReceive();
        	allAttributes.addAll(receiveAttributes);
        	Collection<ConfigAttribute> sendAttributes = policy.getConfigAttributesForSend();
        	allAttributes.addAll(sendAttributes);
        }
        return allAttributes;
	}

	public boolean supports(Class<?> clazz) {
		return ChannelInvocation.class.isAssignableFrom(clazz);
	}

}
