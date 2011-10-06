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

package org.springframework.integration.ws;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;

import org.springframework.integration.mapping.AbstractHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.xml.namespace.QNameUtils;

/**
 * A {@link HeaderMapper} implementation for mapping to and from a SoapHeader.
 * The {@link #inboundHeaderNames} and {@link #outboundHeaderNames} may be configured.
 * They accept exact name Strings or simple patterns (e.g. "start*", "*end", or "*").  
 * By default all inbound headers will be accepted, but any outbound header that should
 * be mapped must be configured explicitly. Note that the outbound mapping only writes
 * String header values into attributes on the SoapHeader. For anything more advanced,
 * one should implement the HeaderMapper interface directly.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class DefaultSoapHeaderMapper extends AbstractHeaderMapper<SoapHeader> {

	public DefaultSoapHeaderMapper(boolean outbound) {
		super(WebServiceHeaders.class, outbound);
	}

	@Override
	protected Map<String, Object> extractStandardHeaders(SoapHeader source) {
		return Collections.emptyMap();
	}

	@Override
	protected Map<String, Object> extractUserDefinedHeaders(SoapHeader source) {
		Map<String, Object> headers = new HashMap<String, Object>();
		if (source != null) {
			Iterator<?> attributeIter = source.getAllAttributes();
			while (attributeIter.hasNext()) {
				Object name = attributeIter.next();
				if (name instanceof QName) {
					String qnameString = QNameUtils.toQualifiedName((QName) name);
					String value = source.getAttributeValue((QName) name);
					if (value != null) {
						headers.put(qnameString, value);
					}
				}
			}
			Iterator<?> elementIter = source.examineAllHeaderElements();
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
	protected void populateStandardHeaders(Map<String, Object> headers,
			SoapHeader target) {
		// no op
	}

	@Override
	protected void populateUserDefinedHeader(String headerName, Object headerValue, SoapHeader target) {
		if (headerValue instanceof String) {
			QName qname = QNameUtils.parseQNameString(headerName);
			target.addAttribute(qname, (String) headerValue);
		}
	}

}
