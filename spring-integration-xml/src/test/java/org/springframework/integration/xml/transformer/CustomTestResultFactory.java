/*
 * Copyright 2016-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.xml.transformer;

import java.io.StringWriter;

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import org.springframework.integration.xml.result.ResultFactory;

public class CustomTestResultFactory implements ResultFactory {

	private final String stringToReturn;

	public CustomTestResultFactory(String stringToReturn) {
		this.stringToReturn = stringToReturn;
	}

	public Result createResult(Object payload) {
		return new FixedStringResult(this.stringToReturn);  //To change body of implemented methods use File | Settings | File Templates.
	}

	public static class FixedStringResult extends StreamResult {

		private final String stringToReturn;

		public FixedStringResult(String stringToReturn) {
			super(new StringWriter());
			this.stringToReturn = stringToReturn;
		}

		/**
		 * Returns the written XML as a string.
		 */
		public String toString() {
			return this.stringToReturn;
		}

	}

}
