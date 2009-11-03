/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.integration.osgi.config.xml;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.integration.osgi.AbstractSIConfigBundleTestDeployer;
import org.springframework.integration.osgi.stubs.SIBundleContextStub;

/**
 * Will make sure only one instance of the Control Bus can exist per single Application Context
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class MultiBusDefinitionInSingleApplicationContextTests extends AbstractSIConfigBundleTestDeployer{
	@Test(expected=BeanDefinitionStoreException.class)
	public void testMultiBusDefinitionConfig() throws Exception {
		BundleContext bundleContext = SIBundleContextStub.getInstance();
		this.deploySIConfig(bundleContext, 
				            "org/springframework/integration/osgi/config/xml/", 
				            "MultiBusDefinitionInSingleApplicationContextTests-context.xml");
	}
}
