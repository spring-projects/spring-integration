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

package org.springframework.integration.xml.selector;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

import org.xml.sax.SAXParseException;

import org.springframework.core.io.Resource;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.xml.AggregatedXmlMessageValidationException;
import org.springframework.integration.xml.DefaultXmlPayloadConverter;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.xml.validation.XmlValidator;
import org.springframework.xml.validation.XmlValidatorFactory;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Liujiong
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class XmlValidatingMessageSelector implements MessageSelector {

	public enum SchemaType {

		XML_SCHEMA(XmlValidatorFactory.SCHEMA_W3C_XML),

		RELAX_NG(XmlValidatorFactory.SCHEMA_RELAX_NG);

		private final String url;

		SchemaType(String url) {
			this.url = url;
		}

		public String getUrl() {
			return this.url;
		}

	}

	private static final LogAccessor LOGGER = new LogAccessor(XmlValidatingMessageSelector.class);

	private final XmlValidator xmlValidator;

	private boolean throwExceptionOnRejection;

	private XmlPayloadConverter converter = new DefaultXmlPayloadConverter();


	/**
	 * Creates a selector with a default {@link XmlValidator}. The validator will be initialized with
	 * the provided 'schema' location {@link Resource} and 'schemaType'. The valid options for schema
	 * type are {@link XmlValidatorFactory#SCHEMA_W3C_XML} or {@link XmlValidatorFactory#SCHEMA_RELAX_NG}.
	 * If no 'schemaType' is provided it will default to {@link XmlValidatorFactory#SCHEMA_W3C_XML};
	 * @param schema The schema.
	 * @param schemaType The schema type.
	 * @throws IOException if the XmlValidatorFactory fails to create a validator
	 */
	public XmlValidatingMessageSelector(Resource schema, SchemaType schemaType) throws IOException {
		this(XmlValidatorFactory.createValidator(schema,
				schemaType == null
						? SchemaType.XML_SCHEMA.getUrl()
						: schemaType.getUrl()));
	}

	public XmlValidatingMessageSelector(XmlValidator xmlValidator) {
		Assert.notNull(xmlValidator, "XmlValidator must not be null");
		this.xmlValidator = xmlValidator;
	}


	public XmlValidatingMessageSelector(Resource schema, String schemaType) throws IOException {
		this(schema,
				StringUtils.hasText(schemaType)
						? SchemaType.valueOf(schemaType.toUpperCase().replaceFirst("-", "_"))
						: null);
	}


	public void setThrowExceptionOnRejection(boolean throwExceptionOnRejection) {
		this.throwExceptionOnRejection = throwExceptionOnRejection;
	}

	/**
	 * Specify the Converter to use when converting payloads prior to validation.
	 * @param converter The payload converter.
	 */
	public void setConverter(XmlPayloadConverter converter) {
		Assert.notNull(converter, "'converter' must not be null");
		this.converter = converter;
	}

	@Override
	public boolean accept(Message<?> message) {
		SAXParseException[] validationExceptions;
		try {
			validationExceptions = this.xmlValidator.validate(this.converter.convertToSource(message.getPayload()));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		boolean validationSuccess = ObjectUtils.isEmpty(validationExceptions);
		if (!validationSuccess) {
			String exceptionMessage = "Message was rejected due to XML Validation errors";
			if (this.throwExceptionOnRejection) {
				throw new MessageRejectedException(message, exceptionMessage,
						new AggregatedXmlMessageValidationException(Arrays.asList(validationExceptions)));
			}
			else {
				LOGGER.info(new AggregatedXmlMessageValidationException(Arrays.asList(validationExceptions)),
						exceptionMessage);
			}
		}
		return validationSuccess;
	}

}
