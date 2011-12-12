/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.http;

import org.springframework.core.SpringVersion;

/**
 * Modified version of the MockHttpServletRequest that sets the "Content-Type"
 * header so that it will be available from a ServletServerHttpRequest instance.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class MockHttpServletRequest extends org.springframework.mock.web.MockHttpServletRequest {

	public MockHttpServletRequest() {
		super();
	}

	public MockHttpServletRequest(String method, String url) {
		super(method, url);
	}


	@Override
	public void setContentType(String contentType) {
		String springVersion = SpringVersion.getVersion();
		if (springVersion != null && springVersion.startsWith("3.0")) {
			this.addHeader("Content-Type", contentType);
		}
		else {
			super.setContentType(contentType);
		}
	}

	@Override
	public String getContentType() {
		return this.getHeader("Content-Type");
	}

}
