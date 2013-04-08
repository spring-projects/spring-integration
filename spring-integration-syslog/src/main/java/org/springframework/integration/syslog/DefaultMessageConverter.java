/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.syslog;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.SyslogToMapTransformer;

/**
 * Default {@link MessageConverter}; delegates to a {@link SyslogToMapTransformer}
 * to convert the payload to a map of values and also provides each field (FACILITY,
 * SEVERITY, TIMESTAMP, HOSTNAME, TAG) as a message header (prefixed by 'syslog_').
 * @author Gary Russell
 * @since 3.0
 *
 */
public class DefaultMessageConverter implements MessageConverter {

	private final SyslogToMapTransformer transformer = new SyslogToMapTransformer();

	@Override
	public Message<?> fromSyslog(Message<?> message) throws Exception {
		Map<String, ?> map = this.transformer.doTransform(message);
		Map<String, Object> out = new HashMap<String, Object>();
		for (Entry<String, ?> entry : map.entrySet()) {
			String key = entry.getKey();
			if (!SyslogToMapTransformer.MESSAGE.equals(key) &&
				!SyslogToMapTransformer.UNDECODED.equals(key)) {
				out.put(PREFIX + entry.getKey(), entry.getValue());
			}
		}
		return MessageBuilder.withPayload(map)
				.copyHeaders(out)
				.build();
	}

}
