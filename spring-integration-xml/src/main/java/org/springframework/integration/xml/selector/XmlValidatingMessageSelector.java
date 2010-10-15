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

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.xml.AggregatedXmlMessageValidationException;
import org.springframework.integration.xml.DefaultXmlPayloadConverter;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.xml.validation.XmlValidator;
import org.springframework.xml.validation.XmlValidatorFactory;
import org.xml.sax.SAXParseException;
/**
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 *
 */
public class XmlValidatingMessageSelector implements MessageSelector {
	
	private final XmlValidator xmlValidator;
	
	private volatile boolean throwExceptionOnRejection;

	private volatile XmlPayloadConverter converter = new DefaultXmlPayloadConverter();
	
	public XmlValidatingMessageSelector(XmlValidator xmlValidator) throws Exception{
		Assert.notNull(xmlValidator, "XmlValidator can not be 'null'");
		this.xmlValidator = xmlValidator;
	}
	/**
	 * Will create this selector with default {@link XmlValidator} which 
	 * will be initialized with 'schema' location as {@link Resource} and 'schemaType' as
	 * either {@link XmlValidatorFactory#SCHEMA_W3C_XML} or {@link XmlValidatorFactory#SCHEMA_RELAX_NG}.
	 * If no 'schemaType' is provided it will default to {@link XmlValidatorFactory#SCHEMA_W3C_XML};
	 * 
	 * @param schema
	 * @param schemaType
	 * @throws IOException
	 */
	public XmlValidatingMessageSelector(Resource schema, String schemaType) throws IOException {
		Assert.notNull(schema, "You must provide XML schema location to perform validation");
		if (!StringUtils.hasText(schemaType)){
			schemaType = XmlValidatorFactory.SCHEMA_W3C_XML;
		}
		this.xmlValidator = XmlValidatorFactory.createValidator(schema, schemaType);
	}
	
	public void setThrowExceptionOnRejection(boolean throwExceptionOnRejection) {
		this.throwExceptionOnRejection = throwExceptionOnRejection;
	}
	
	/**
	 * Converter used to convert payloads prior to validation
	 * 
	 * @param converter
	 */
	public void setConverter(XmlPayloadConverter converter) {
		Assert.notNull(converter, "'converter' must not be null");
		this.converter = converter;
	}
	
	@SuppressWarnings("unchecked")
	public boolean accept(Message<?> message) {
		SAXParseException[] validationExceptions = null;
		try {
			validationExceptions = xmlValidator.validate(converter.convertToSource(message.getPayload()));
		} catch (Exception e) {
			throw new MessageHandlingException(message, e);
		}
		boolean validationSuccess = ObjectUtils.isEmpty(validationExceptions);
		if (!validationSuccess && throwExceptionOnRejection){
			throw new MessageRejectedException(message, "Message was rejected due to XML Validation errors", 
					new AggregatedXmlMessageValidationException(CollectionUtils.arrayToList(validationExceptions)));
		}
		return validationSuccess;
	}
}
