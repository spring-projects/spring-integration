/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.gemfire.outbound;

import java.util.Map;

import org.springframework.data.gemfire.GemfireCallback;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessageHandler;
import org.springframework.util.Assert;

import com.gemstone.gemfire.GemFireCheckedException;
import com.gemstone.gemfire.GemFireException;
import com.gemstone.gemfire.cache.Region;

/**
 * A {@link MessageHandler} implementation that writes to a GemFire Region.
 * The Message's payload must be an instance of java.util.Map.
 * 
 * @author Mark Fisher
 * @since 2.1
 */
public class CacheWritingMessageHandler implements MessageHandler {

	private final GemfireTemplate gemfireTemplate = new GemfireTemplate();


	@SuppressWarnings("rawtypes")
	public CacheWritingMessageHandler(Region region) {
		Assert.notNull(region, "region must not be null");
		this.gemfireTemplate.setRegion(region);
		this.gemfireTemplate.afterPropertiesSet();
	}


	public void handleMessage(Message<?> message) {
		// TODO: add support for more options to get key/value (SpEL?)
		Object payload = message.getPayload();
		Assert.isTrue(payload instanceof Map, "only Map payloads are supported");
		final Map<?, ?> map = (Map<?, ?>) payload;
		this.gemfireTemplate.execute(new GemfireCallback<Object>() {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			public Object doInGemfire(Region region) throws GemFireCheckedException, GemFireException {
				region.putAll(map);
				return null;
			}
		});
	}

}
