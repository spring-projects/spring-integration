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

package org.springframework.integration.jms.config;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Message;

import org.springframework.messaging.MessageHeaders;
import org.springframework.integration.jms.JmsHeaderMapper;

/**
 * @author Mark Fisher
 */
public class TestJmsHeaderMapper implements JmsHeaderMapper {

	public void fromHeaders(MessageHeaders headers, Message target) {
	}

	public Map<String, Object> toHeaders(Message source) {
		Map<String, Object> headerMap = new HashMap<String, Object>();
		headerMap.put("testProperty", "foo");
		headerMap.put("testAttribute", new Integer(123));
		return headerMap;
	}

}
