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

package org.springframework.integration.xml.transformer;

import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Transformer;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageHeaders;

/**
 * {@link TransformerConfigurer} instance which looks for headers and uses them
 * to configure the provided {@link Transformer} instance. For example a header
 * named 'xslt_parameter_X' will cause the transformer to be configured with a
 * property named 'X' with the value of the header. A property named
 * 'xslt_output_property_X' will cause an output property on the transformer to be
 * set with this header's value.
 * 
 * @author Jonas Partner
 */
public class DefaultTransformerConfigurer implements TransformerConfigurer {

	public void configureTransformer(Message<?> message, Transformer transformer) {
		Map<String,Object> parameters = extractParameterHeaders(message.getHeaders());
		for (String paramName: parameters.keySet()) {
			transformer.setParameter(paramName, parameters.get(paramName));
		}
		Map<String, String> outputProperties = extractOutputPropertyHeaders(message.getHeaders());
		for (String outputPropertyName : outputProperties.keySet()) {
			transformer.setOutputProperty(outputPropertyName, outputProperties.get(outputPropertyName));
		}
	}

	protected Map<String, String> extractOutputPropertyHeaders(MessageHeaders headers) {
		Map<String, String> parameters = new HashMap<String, String>();
		int prefixStringLength = XsltHeaders.OUTPUT_PROPERTY.length();
		for (String key : headers.keySet()) {
			if (key.startsWith(XsltHeaders.OUTPUT_PROPERTY)) {
				Object headerValue = headers.get(key);
				if (!(headerValue instanceof String)) {
					throw new IllegalArgumentException(
							"XSLT Transformer only supports String output properties received header of type "
									+ headerValue.getClass().getName()
									+ " for header named " + key);
				}
				parameters.put(key.substring(prefixStringLength), (String) headerValue);
			}
		}
		return parameters;
	}

	protected Map<String, Object> extractParameterHeaders(MessageHeaders headers) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		int prefixStringLength = XsltHeaders.PARAMETER.length();
		for (String key : headers.keySet()) {
			if (key.startsWith(XsltHeaders.PARAMETER)) {
				parameters.put(key.substring(prefixStringLength), headers.get(key));
			}
		}
		return parameters;
	}

}
