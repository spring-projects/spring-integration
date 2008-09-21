package org.springframework.integration.file.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.file.PollableFileSource;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

public class FileInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected String parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(PollableFileSource.class);
		String directory = element.getAttribute("directory");
		if (StringUtils.hasText(directory)) {
			builder.addPropertyValue("inputDirectory", directory);
		}
		String filter = element.getAttribute("filter");
		if (StringUtils.hasText(filter)){
			builder.addPropertyReference("filter", filter);
		}
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

}
