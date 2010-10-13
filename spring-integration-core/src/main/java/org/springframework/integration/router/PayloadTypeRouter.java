/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.router;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A Message Router that resolves the {@link MessageChannel} based on the
 * {@link Message Message's} payload type.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class PayloadTypeRouter extends AbstractMessageRouter {

	@Override
	protected List<Object> getChannelIndicatorList(Message<?> message) {
		Class<?> firstInterfaceMatch = null;
		Class<?> type = message.getPayload().getClass();
		
		while (type != null && channelIdentifierMap != null) {
			Class<?>[] interfaces = type.getInterfaces();
			// first try to find a match amongst the interfaces and also check if there is more then one
			for (Class<?> interfase : interfaces) {
				if (channelIdentifierMap.containsKey(interfase.getName())){
					if (firstInterfaceMatch != null){
						throw new IllegalStateException("Unresolvable ambiguity while attempting to find closest match for [" +
								type.getName() + "]. Candidate types [" + firstInterfaceMatch.getName() + "] and [" + interfase.getName() + 
								"] have equal weight.");
					}
					else {
						firstInterfaceMatch = interfase;
					}
				}
			}
			// the actual type should favor the possible interface match
			String channelName = channelIdentifierMap.get(type.getName());
			if (!StringUtils.hasText(channelName)){
				if (firstInterfaceMatch != null){
					return Collections.singletonList((Object)channelIdentifierMap.get(firstInterfaceMatch.getName()));
				}	
			}
			else {
				return Collections.singletonList((Object)channelName);
			}
			type = type.getSuperclass();
		}
		return null;
	}
}
