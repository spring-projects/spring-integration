/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.ws;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.mapping.AbstractHeaderMapper;
import org.springframework.util.StringUtils;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.SoapHeaderException;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.xml.namespace.QNameUtils;
import org.springframework.xml.transform.TransformerHelper;

/**
 * A {@link org.springframework.integration.mapping.HeaderMapper} implementation for
 * mapping to and from a SoapHeader.
 * The {@link #setRequestHeaderNames(String[])} and {@link #setReplyHeaderNames(String[])}
 * accept exact name Strings or simple patterns (e.g. "start*", "*end", or "*").
 * By default all inbound headers will be accepted, but any outbound header that should
 * be mapped must be configured explicitly. Note that the outbound mapping only writes
 * String header values into attributes on the SoapHeader. For anything more advanced,
 * one should implement the HeaderMapper interface directly.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Stephane Nicoll
 * @author Mauro Molinari
 * @author Artem Bilan
 * @author Gary Russell
 * @author Jooyoung Pyoung
 * @author Glenn Renfro
 *
 * @since 2.0
 */
public class DefaultSoapHeaderMapper extends AbstractHeaderMapper<SoapMessage> implements SoapHeaderMapper {

	protected static final List<String> STANDARD_HEADER_NAMES = new ArrayList<>();

	static {
		STANDARD_HEADER_NAMES.add(WebServiceHeaders.SOAP_ACTION);
	}

	protected final TransformerHelper transformerHelper = new TransformerHelper(); // NOSONAR final

	public DefaultSoapHeaderMapper() {
		super(WebServiceHeaders.PREFIX, STANDARD_HEADER_NAMES, Collections.emptyList());
	}

	@Override
	protected Map<String, @Nullable Object> extractStandardHeaders(SoapMessage source) {
		final String soapAction = source.getSoapAction();
		if (StringUtils.hasText(soapAction)) {
			Map<String, @Nullable Object> headers = new HashMap<>(1);
			headers.put(WebServiceHeaders.SOAP_ACTION, soapAction);
			return headers;
		}
		else {
			return Collections.emptyMap();
		}
	}

	@Override
	protected Map<String, @Nullable Object> extractUserDefinedHeaders(SoapMessage source) {
		Map<String, @Nullable Object> headers = new HashMap<>();
		SoapHeader soapHeader = source.getSoapHeader();
		if (soapHeader != null) {
			Iterator<?> attributeIter = soapHeader.getAllAttributes();
			while (attributeIter.hasNext()) {
				Object name = attributeIter.next();
				if (name instanceof QName) {
					String qnameString = QNameUtils.toQualifiedName((QName) name);
					headers.put(qnameString, soapHeader.getAttributeValue((QName) name));
				}
			}
			Iterator<?> elementIter = soapHeader.examineAllHeaderElements();
			while (elementIter.hasNext()) {
				Object element = elementIter.next();
				if (element instanceof SoapHeaderElement) {
					QName qname = ((SoapHeaderElement) element).getName();
					String qnameString = QNameUtils.toQualifiedName(qname);
					headers.put(qnameString, element);
				}
			}
		}
		return headers;
	}

	@Override
	protected void populateStandardHeaders(Map<String, Object> headers, SoapMessage target) {
		String soapAction = getHeaderIfAvailable(headers, WebServiceHeaders.SOAP_ACTION, String.class);
		if (StringUtils.hasText(soapAction)) {
			target.setSoapAction(soapAction);
		}
		else if (!StringUtils.hasText(target.getSoapAction())) {
			target.setSoapAction("\"\"");
		}
	}

	@Override
	protected void populateUserDefinedHeader(String headerName, Object headerValue, SoapMessage target) {
		SoapHeader soapHeader = target.getSoapHeader();
		if (soapHeader == null) {
			return;
		}

		if (headerValue instanceof String) {
			QName qname = QNameUtils.parseQNameString(headerName);
			soapHeader.addAttribute(qname, (String) headerValue);
		}
		else if (headerValue instanceof Source) {
			Result result = soapHeader.getResult();
			try {
				this.transformerHelper.transform((Source) headerValue, result);
			}
			catch (TransformerException e) {
				throw new SoapHeaderException(
						"Could not transform source [" + headerValue + "] to result [" + result + "]", e);
			}
		}
	}

}
