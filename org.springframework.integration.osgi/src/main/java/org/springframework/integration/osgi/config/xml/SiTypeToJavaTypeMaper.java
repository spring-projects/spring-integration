/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.integration.osgi.config.xml;

import java.util.HashMap;
import java.util.Map;

import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.util.Assert;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
@SuppressWarnings("unchecked")
class SiTypeToJavaTypeMaper {
	public static final String PUB_SUB_CHANNEL = "publish-subscribe-channel";
	private static Map<String, Class[]>  siTypeMappings = new HashMap<String, Class[]>();
	
	static {
		siTypeMappings.put(PUB_SUB_CHANNEL, new Class[]{SubscribableChannel.class});
	}
	
	public static Class[] mapSiType(String siType){
		Assert.isTrue(siTypeMappings.containsKey(siType), "Can not map SI-Type '" + siType + "' to Java Type. Not supported.");
		return siTypeMappings.get(siType);
	}
}
