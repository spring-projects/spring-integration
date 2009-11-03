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
package org.springframework.integration.osgi;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;

import org.osgi.framework.BundleContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.osgi.stubs.SIBundleContextStub;
import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;
import org.springframework.osgi.mock.ArrayEnumerator;
import org.springframework.osgi.mock.MockBundle;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public abstract class AbstractSIConfigBundleTestDeployer {
	protected OsgiBundleXmlApplicationContext applicationContext;
	
	@SuppressWarnings("unchecked")
	public ConfigurableOsgiBundleApplicationContext deploySIConfig(BundleContext bundleContext, String configPackage, String... configFiles) throws Exception {
		applicationContext = new OsgiBundleXmlApplicationContext(configFiles);
		final ArrayList<URL> tempConfigurations = new ArrayList<URL>();
		for (String configFile : configFiles) {
			tempConfigurations.add(new ClassPathResource(configPackage + configFile).getURL());
		}
		((SIBundleContextStub)bundleContext).setBundle(new MockBundle() {
			public Enumeration findEntries(String path, String filePattern, boolean recurse) {
				return new ArrayEnumerator(tempConfigurations.toArray());
			}
		});
		((OsgiBundleXmlApplicationContext)applicationContext).setBundleContext(bundleContext);
		((OsgiBundleXmlApplicationContext)applicationContext).refresh();
		return applicationContext;
	}
}
