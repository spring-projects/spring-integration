/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.integration.config.xml.HeaderEnricherParserSupport;
import org.springframework.jms.support.JmsHeaders;

/**
 * Header enricher for JMS specific values.
 *
 * @author Mark Fisher
 * @since 2.0
 */
public class JmsHeaderEnricherParser extends HeaderEnricherParserSupport {

	public JmsHeaderEnricherParser() {
		this.addElementToHeaderMapping("correlation-id", JmsHeaders.CORRELATION_ID);
		this.addElementToHeaderMapping("reply-to", JmsHeaders.REPLY_TO);
	}

}
