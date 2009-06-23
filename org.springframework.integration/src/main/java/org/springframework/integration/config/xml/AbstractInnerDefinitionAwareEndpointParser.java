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

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
/**
 * Abstract Parser for any consumers that require capabilities to define
 * its handler implementation as inner bean.<br>
 * For example:<br>
 * <pre>
 * &lt;transformer id="testTransformer" input-channel="inChannel" output-channel="outChannel"&gt;
 *		&lt;beans:bean class="org.bar.TestTransformer"/&gt;
 * &lt;/transformer&gt;
 * </pre>
 *	
 * @author Oleg Zhurakousky
 * @since 1.0.3
 */
public abstract class AbstractInnerDefinitionAwareEndpointParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		// parses out inner bean definition for concrete implementation if defined
		List<Element> childElements = DomUtils.getChildElementsByTagName(element, "bean");
		BeanDefinition innerDefinition = null;
		if (childElements != null && childElements.size() == 1){
			Element beanElement = childElements.get(0);
			BeanDefinitionParserDelegate delegate = parserContext.getDelegate();
			innerDefinition = delegate.parseBeanDefinitionElement(beanElement).getBeanDefinition();
		}
		
		String ref = element.getAttribute(REF_ATTRIBUTE);
		Assert.isTrue(!(StringUtils.hasText(ref) && innerDefinition != null), "Ambiguous definition. Inner bean " + 
				(innerDefinition == null ? innerDefinition : innerDefinition.getBeanClassName()) + " declaration and \"ref\" " + ref + 
		       " are not allowed together.");
		return this.parseEndpoint(element, parserContext, innerDefinition);
	}
	/**
	 * 
	 * @param element
	 * @param parserContext
	 * @param innerDefinition
	 * @return
	 */
	protected abstract BeanDefinitionBuilder parseEndpoint(Element element, ParserContext parserContext, BeanDefinition innerDefinition);
}
