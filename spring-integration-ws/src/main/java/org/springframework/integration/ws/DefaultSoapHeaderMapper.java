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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;

import org.springframework.integration.MessageHeaders;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
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
 * @since 2.0
 */
public class DefaultSoapHeaderMapper implements HeaderMapper<SoapHeader> {

	private volatile String[] outboundHeaderNames = new String[0];

	private volatile String[] inboundHeaderNames = new String[] { "*" };


	public void setOutboundHeaderNames(String[] outboundHeaderNames) {
		this.outboundHeaderNames = (outboundHeaderNames != null) ? outboundHeaderNames : new String[0];
	}

	public void setInboundHeaderNames(String[] inboundHeaderNames) {
		this.inboundHeaderNames = (inboundHeaderNames != null) ? inboundHeaderNames : new String[0];
	}

	public void fromHeaders(MessageHeaders headers, SoapHeader target) {
		if (target != null && !CollectionUtils.isEmpty(headers)) {
			for (String headerName : headers.keySet()) {
				if (this.shouldMapOutboundHeader(headerName)) {
					Object value = headers.get(headerName);
					if (value instanceof String) {
						QName qname = QNameUtils.parseQNameString(headerName);
						target.addAttribute(qname, (String) value);
					}
				}
			}
		}
	}

	public Map<String, Object> toHeaders(SoapHeader source) {
		Map<String, Object> headers = new HashMap<String, Object>();
		if (source != null) {
			Iterator<?> attributeIter = source.getAllAttributes();
			while (attributeIter.hasNext()) {
				Object name = attributeIter.next();
				if (name instanceof QName) {
					String qnameString = QNameUtils.toQualifiedName((QName) name);
					if (this.shouldMapInboundHeader(qnameString)) {
						String value = source.getAttributeValue((QName) name);
						if (value != null) {
							headers.put(qnameString, value);
						}
					}
				}
			}
			Iterator<?> elementIter = source.examineAllHeaderElements();
			while (elementIter.hasNext()) {
				Object element = elementIter.next();
				if (element instanceof SoapHeaderElement) {
					QName qname = ((SoapHeaderElement) element).getName();
					String qnameString = QNameUtils.toQualifiedName(qname);
					if (this.shouldMapInboundHeader(qnameString)) {
						headers.put(qnameString, element);
					}
				}
			}
		}
		return headers;
	}

	private boolean shouldMapInboundHeader(String headerName) {
		return matchesAny(this.inboundHeaderNames, headerName);
	}

	private boolean shouldMapOutboundHeader(String headerName) {
		return matchesAny(this.outboundHeaderNames, headerName);
	}

	private static boolean matchesAny(String[] patterns, String candidate) {
		if (!ObjectUtils.isEmpty(patterns) && QNameUtils.validateQName(candidate)) {
			for (String pattern : patterns) {
				if (PatternMatchUtils.simpleMatch(pattern, candidate)) {
					return true;
				}
			}
		}
		return false;
	}

}
