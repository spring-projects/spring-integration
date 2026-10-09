/*
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.zookeeper.config.xml;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * @author Gary Russell
 * @since 4.2
 *
 */
public class ZookeeperNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	@Override
	public void init() {
		this.registerBeanDefinitionParser("leader-listener", new LeaderListenerParser());
	}

}
