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

/**
 * A container for defining a message port configuration used in a {@link Flow}.
 * Binds a logical port name to a channel defined internally by the flow
 * @author David Turanski
 * 
 */
public class PortMetadata  {

	private final String channelName;
	private final String portName;

	public PortMetadata(String portName, String channelName) {
		 this.portName = portName;
		 this.channelName = channelName;
	}

    public String getChannelName() {
        return channelName;
    }

    public String getPortName() {
        return portName;
    }

	 

	 
}
