/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jms.config;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Message;

import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.messaging.MessageHeaders;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class TestJmsHeaderMapper extends JmsHeaderMapper {

	@Override
	public void fromHeaders(MessageHeaders headers, Message target) {
	}

	@Override
	public Map<String, Object> toHeaders(Message source) {
		Map<String, Object> headerMap = new HashMap<>();
		headerMap.put("testProperty", "foo");
		headerMap.put("testAttribute", 123);
		return headerMap;
	}

}
