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

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.osgi.AbstractSIConfigBundleTestDeployer;
import org.springframework.integration.osgi.stubs.SIBundleContextStub;

/**
 * Tests 'config' element.
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class ConfigParserImporterTests extends AbstractSIConfigBundleTestDeployer {

	@Test
	public void testBasicSIServiceConfig() throws Exception {
		BundleContext bundleContext = SIBundleContextStub.getNewInstance();
		ApplicationContext ac = this.deploySIConfig(bundleContext, 
				                                    "org/springframework/integration/osgi/config/xml/", 
				                                    "ConfigParserImporterTests-default.xml");
		assertNotNull(ac.getBean("channelA"));
	

	}
}
