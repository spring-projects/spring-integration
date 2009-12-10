/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Base class for channel parsers.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractChannelParser extends AbstractBeanDefinitionParser {

	@Override
	@SuppressWarnings("unchecked")
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = this.buildBeanDefinition(element, parserContext);
		ManagedList interceptors = null;
		Element interceptorsElement = DomUtils.getChildElementByTagName(element, "interceptors");
		if (interceptorsElement != null) {
			ChannelInterceptorParser interceptorParser = new ChannelInterceptorParser();
			interceptors = interceptorParser.parseInterceptors(interceptorsElement, parserContext);
		}
		if (interceptors == null) {
			interceptors = new ManagedList();
		}
		String datatypeAttr = element.getAttribute("datatype");
		if (StringUtils.hasText(datatypeAttr)) {
			// TODO: remove this once the editor fallback is working (3.0 GA)
			//       it should be replaced with: builder.addPropertyValue("datatypes", datatypeAttr);
			String[] classnames = StringUtils.commaDelimitedListToStringArray(datatypeAttr);
			Class<?>[] datatypes = new Class<?>[classnames.length];
			int i = 0;
			for (String classname : classnames) {
				datatypes[i++] = ClassUtils.resolveClassName(classname.trim(), this.getClass().getClassLoader());
			}
			builder.addPropertyValue("datatypes", datatypes);
		}
		builder.addPropertyValue("interceptors", interceptors);
		return builder.getBeanDefinition();
	}

	/**
	 * Subclasses must implement this method to create the bean definition.
	 * The class must be defined, and any implementation-specific constructor
	 * arguments or properties should be configured. This base class will
	 * configure the interceptors including the 'datatype' interceptor if
	 * the 'datatype' attribute is defined on the channel element.
	 */
	protected abstract BeanDefinitionBuilder buildBeanDefinition(Element element, ParserContext parserContext);

}
