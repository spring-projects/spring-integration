/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.kafka.util;

/**
 * Utilities for logging data
 *
 * @author Marius Bogoevici
 */
public class LoggingUtils {

	public static String asCommaSeparatedHexDump(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return "[]";
		}
		else if (bytes.length == 1) {
			return String.format("[%s]", Integer.toHexString(bytes[0]));
		}
		else {
			StringBuilder buffer = new StringBuilder("[");
			for (int i = 0; i < bytes.length - 1; i++) {
				buffer.append(Integer.toHexString(bytes[i]));
				buffer.append(",");
			}
			buffer.append(Integer.toHexString(bytes[bytes.length]));
			buffer.append("]");
			return buffer.toString();
		}
	}
	
}
