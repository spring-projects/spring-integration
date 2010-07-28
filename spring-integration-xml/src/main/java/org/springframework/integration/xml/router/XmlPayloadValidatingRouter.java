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

package org.springframework.integration.xml.router;

import org.springframework.integration.Message;
import org.springframework.integration.router.AbstractSingleChannelNameRouter;
import org.springframework.integration.xml.DefaultXmlPayloadConverter;
import org.springframework.integration.xml.XmlPayloadConverter;

public class XmlPayloadValidatingRouter extends AbstractSingleChannelNameRouter{

	private final String validMessageChannelName;
	
	private final String invalidMessageChannelName;
	
	private final XmlValidator xmlValidator;
	
	private volatile XmlPayloadConverter converter = new DefaultXmlPayloadConverter();
	
	
	public XmlPayloadValidatingRouter(String validMessageChannelName,
			String invalidMessageChannelName, XmlValidator xmlValidator) {
		super();
		this.validMessageChannelName = validMessageChannelName;
		this.invalidMessageChannelName = invalidMessageChannelName;
		this.xmlValidator = xmlValidator;
	}
	
	/**
	 * Converter used to convert payloads prior to validation
	 * 
	 * @param converter
	 */
	public void setConverter(XmlPayloadConverter converter) {
		this.converter = converter;
	}


	@Override
	protected String determineTargetChannelName(Message<?> message) {
		return xmlValidator.isValid(converter.convertToSource(message.getPayload())) ? validMessageChannelName : invalidMessageChannelName;
	}




}
