/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.util;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * @author Artem Bilan
 *
 * @since 5.1
 */
public class NoBeansOverrideAnnotationConfigContextLoader extends AnnotationConfigContextLoader {

	@Override
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
		beanFactory.setAllowBeanDefinitionOverriding(false);
	}

}
