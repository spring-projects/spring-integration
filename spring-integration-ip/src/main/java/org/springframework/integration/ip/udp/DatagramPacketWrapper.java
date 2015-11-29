/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.integration.ip.udp;

import java.net.DatagramPacket;
import java.util.Map;

/**
 * TODO
 *
 * @author Marcin Pilaczynski
 * @since 4.3
 */
public class DatagramPacketWrapper {

	private DatagramPacket datagramPacket;

	private Map<String, ?> additionalHeaders;

	public DatagramPacketWrapper(DatagramPacket datagramPacket, Map<String, ?> additionalHeaders) {

		this.datagramPacket = datagramPacket;
		this.additionalHeaders = additionalHeaders;
	}

	public DatagramPacket getDatagramPacket() {
		return datagramPacket;
	}

	public Map<String, ?> getAdditionalHeaders() {
		return additionalHeaders;
	}
}
