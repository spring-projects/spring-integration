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

package org.springframework.integration.xml.selector;

import org.springframework.core.io.Resource;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.xml.DefaultXmlPayloadConverter;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.util.Assert;
import org.springframework.xml.validation.XmlValidator;
import org.springframework.xml.validation.XmlValidatorFactory;
import org.xml.sax.SAXParseException;
/**
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 *
 */
public class SchemaValidatingMessageSelector implements MessageSelector{
	
	private final XmlValidator xmlValidator;
	private volatile String schemaType = XmlValidatorFactory.SCHEMA_W3C_XML;

	private volatile XmlPayloadConverter converter = new DefaultXmlPayloadConverter();
	
	
	public SchemaValidatingMessageSelector(Resource schema) throws Exception{
		Assert.notNull(schema, "You must provide XML schema location to perform validation");
		this.xmlValidator = XmlValidatorFactory.createValidator(schema, schemaType);
	}
	
	/**
	 * Converter used to convert payloads prior to validation
	 * 
	 * @param converter
	 */
	public void setConverter(XmlPayloadConverter converter) {
		this.converter = converter;
	}
	
	public void setSchemaType(String schemaType) {
		this.schemaType = schemaType;
	}

	@Override
	public boolean accept(Message<?> message) {
		// TODO Need to figure out how the exceptions could be propagated since the return from this method is true/false
		// and 'throw-exception-on-rejection'is actually set on the filter
		try {
			SAXParseException[] validationExceptions = xmlValidator.validate(converter.convertToSource(message.getPayload()));
			return validationExceptions.length == 0 ? true : false;
		} catch (Exception e) {
			e.printStackTrace();
			throw new MessageHandlingException(message, e);
		}
	}
}
