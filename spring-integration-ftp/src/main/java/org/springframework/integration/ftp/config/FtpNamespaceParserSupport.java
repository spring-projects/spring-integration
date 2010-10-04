package org.springframework.integration.ftp.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.w3c.dom.Element;


/**
 * A lot of parsers need to support the same set of core attributes, so I'm hiding that logic here
 *
 * @author Josh Long
 *
 */
public class FtpNamespaceParserSupport {
    /**
     * lots of values are supported across all adapters, let this code handle it initially
     *
     * @param builder       a builder
     * @param element       an element
     * @param parserContext a parser context
     */
    public static void configureCoreFtpClient(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
        for (String p : "auto-create-directories,username,port,password,host,remote-directory".split(",")) {
            IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, p);
        }

        if (element.hasAttribute("file-type")) {
            int fileType = FtpNamespaceHandler.FILE_TYPES.get(element.getAttribute("file-type"));
            builder.addPropertyValue("fileType", fileType);
        }

        if (element.hasAttribute("client-mode")) {
            int clientMode = FtpNamespaceHandler.CLIENT_MODES.get(element.getAttribute("client-mode"));
            builder.addPropertyValue("clientMode", clientMode);
        }
    }
}
