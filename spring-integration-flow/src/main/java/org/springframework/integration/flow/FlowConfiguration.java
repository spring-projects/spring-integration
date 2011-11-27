/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.integration.flow;

import java.util.List;

/**
 * A container holding a {@link Flow} configuration. A flow configuration 
 * may contain multiple {@link PortConfiguration}s
 * 
 * @author David Turanski
 * 
 */
public class FlowConfiguration {

	private final List<PortConfiguration> portConfigurations;
	
	/**
	 * 
	 * @param portConfigurations  
	 */ 
	public FlowConfiguration(List<PortConfiguration> portConfigurations) {
		this.portConfigurations = portConfigurations;
	}

	/**
	 * Get the configuration by input port name
	 * @param inputPortName
	 * @return
	 */
	public PortConfiguration getConfigurationForInputPort(String inputPortName) {
		for (PortConfiguration pc : portConfigurations) {
			if (pc.getInputPortName().equals(inputPortName)) {
				return pc;
			}
		}
		return null;
	}
	/**
	 * Get all port configurations
	 * @return the port configurations
	 */
	public List<PortConfiguration> getPortConfigurations() {
		return portConfigurations;
	}

}
