/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.flow.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.util.ResourceUtils;

/**
 * Utility functions used by the flow parsers
 * 
 * @author David Turanski
 * 
 */
public class FlowUtils {

	private FlowUtils() {
	}

	/**
	 * Create a bridge
	 * 
	 * @param inputChannel
	 * @param outputChannel
	 */

	public static void bridgeChannels(SubscribableChannel inputChannel, MessageChannel outputChannel) {
		BridgeHandler bridgeHandler = new BridgeHandler();
		bridgeHandler.setOutputChannel(outputChannel);
		inputChannel.subscribe(bridgeHandler);
	}

	/**
	 * Register a bean with "flow" prefix
	 * 
	 * @param beanDefinition
	 * @param registry
	 * @return
	 */
	public static String registerBeanDefinition(BeanDefinition beanDefinition, BeanDefinitionRegistry registry) {
		String beanName = BeanDefinitionReaderUtils.generateBeanName(beanDefinition, registry);
		beanName = "flow." + beanName;
		String strIndex = org.apache.commons.lang.StringUtils.substringAfter(beanName, "#");
		int index = Integer.valueOf(strIndex);
		while (registry.isBeanNameInUse(beanName)) {
			index++;
			beanName = beanName.replaceAll("#\\d$", "#" + (index));
		}
		registry.registerBeanDefinition(beanName, beanDefinition);
		return beanName;
	}

	/**
	 * Read the flow documentation resource into a String if it exists. The
	 * location is classpath:META-INF/spring/integration/flows/[flowId]/flow.doc
	 * @param flowId the flow id
	 * @return the documentation
	 */
	public static String getDocumentation(String flowId) {

		String path = String.format("classpath:META-INF/spring/integration/flows/%s/flow.doc", flowId);

		try {
			File file = ResourceUtils.getFile(path);

			BufferedReader br = new BufferedReader(new FileReader(file));

			String line;
			StringBuilder result = new StringBuilder();
			while ((line = br.readLine()) != null) {
				result.append(line).append("\n");
			}

			br.close();

			return result.toString();

		}
		catch (FileNotFoundException e) {
			return "no help available";
		}
		catch (IOException e) {
			e.printStackTrace();
			return "no help available";
		}
	}

	public static void displayBeansGraph(ConfigurableListableBeanFactory beanFactory) {
		String[] beans = beanFactory.getBeanNamesForType(Object.class);
		_displayDependencies(beanFactory, beans, 0);
	}

	private static void _displayDependencies(ConfigurableListableBeanFactory beanFactory, String[] beans, int level) {
		for (int i = 0; i < beans.length; i++) {

			System.out.println(indent(level) + beans[i]);

			String[] dependencies = beanFactory.getDependenciesForBean(beans[i]);
			String[] depsArray = new String[dependencies.length];
			int index = 0;
			for (String dependency : dependencies) {
				if (!dependency.equals(beans[i])) {

					depsArray[index++] = dependency;
				}
				else {
					System.out.println(indent(level + 1) + beans[i]);
				}
			}
			if (depsArray.length > 0) {
				_displayDependencies(beanFactory, Arrays.copyOf(depsArray, index), level + 1);
			}

		}

	}

	public static Set<String> getReferencedMessageChannels(ConfigurableListableBeanFactory beanFactory) {
		String[] beans = beanFactory.getBeanNamesForType(Object.class);
		Set<String> messageChannels = new HashSet<String>();
		_getReferencedMessageChannels(beanFactory, beans, messageChannels);
		return Collections.unmodifiableSet(messageChannels);
	}

	private static void _getReferencedMessageChannels(ConfigurableListableBeanFactory beanFactory, String[] beans,
			Set<String> messageChannels) {

		for (String bean : beans) {
			if (!bean.startsWith("(inner bean)") && !bean.equals("nullChannel")) {
				Class<?> clazz = null;
				if (beanFactory.containsBean(bean)) {
					clazz = beanFactory.getType(bean);
				}

				if (clazz != null) {
					if (MessageChannel.class.isAssignableFrom(clazz)) {
						messageChannels.add(bean);
					}
					String[] dependencies = beanFactory.getDependenciesForBean(bean);
					if (dependencies.length > 0) {
						_getReferencedMessageChannels(beanFactory, dependencies, messageChannels);
					}
				}
			}
		}
	}

	private static String indent(int size) {
		String indent = "";
		for (int i = 0; i < size; i++) {
			indent += "  ";
		}
		return indent;

	}

}
