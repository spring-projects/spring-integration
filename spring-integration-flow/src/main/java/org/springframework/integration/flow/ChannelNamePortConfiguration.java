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
/**
 * @author David Turanski
 */
package org.springframework.integration.flow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Container for {@link Flow} {@link PortConfiguration} metadata
 * @author David Turanski
 * 
 */
public class ChannelNamePortConfiguration implements PortConfiguration {

	private PortMetadata inputPortMetadata;

	private List<PortMetadata> outputPortMetadataList;

	/**
	 * Generic configuration
	 * @param inputPortMetadata metadata defining the input message port
	 * @param outputPortMetadataList a list of metadata defining multiple output
	 * message ports
	 */
	public ChannelNamePortConfiguration(PortMetadata inputPortMetadata, List<PortMetadata> outputPortMetadataList) {
		this.outputPortMetadataList = outputPortMetadataList;
		this.inputPortMetadata = inputPortMetadata;
	}

	/**
	 * A simple configuration for a flow with one input and one output port
	 * @param inputChannelName
	 * @param outputChannelName
	 */
	public ChannelNamePortConfiguration(String inputChannelName, String outputChannelName) {
		this.inputPortMetadata = new PortMetadata("input", inputChannelName);

		if (outputChannelName != null) {
			PortMetadata outputPortMetadata = new PortMetadata("output", outputChannelName);
			this.outputPortMetadataList = Collections.singletonList(outputPortMetadata);
		}
		else {
			// this.outputPortMetadataList = new ArrayList<PortMetadata>();
		}
	}

	public String getInputPortName() {
		return this.inputPortMetadata.getPortName();
	}

	public String getInputChannel() {
		return this.inputPortMetadata.getChannelName();
	}

	public String getOutputChannel(String portName) {
		PortMetadata portMetadata = findOutputPort(portName);
		if (portMetadata != null) {
			return portMetadata.getChannelName();
		}
		return null;
	}

	public List<String> getOutputPortNames() {
		List<String> results = new ArrayList<String>();
		if (outputPortMetadataList != null) {
			for (PortMetadata portMetadata : outputPortMetadataList) {
				results.add(portMetadata.getPortName());
			}
		}
		return results;
	}

	private PortMetadata findOutputPort(String portName) {
		if (outputPortMetadataList != null) {
			for (PortMetadata portMetadata : outputPortMetadataList) {
				if (portName.equals(portMetadata.getPortName())) {
					return portMetadata;
				}
			}
		}
		return null;
	}

}
