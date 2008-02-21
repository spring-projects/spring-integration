/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.adapter.stream;

import java.io.InputStream;

import org.springframework.integration.adapter.PollingSourceAdapter;

/**
 * A polling source adapter that wraps a {@link ByteStreamSource}.
 * 
 * @author Mark Fisher
 */
public class ByteStreamSourceAdapter extends PollingSourceAdapter<byte[]> {

	public ByteStreamSourceAdapter(InputStream stream) {
		super(new ByteStreamSource(stream));
	}


	public void setBytesPerMessage(int bytesPerMessage) {
		((ByteStreamSource) this.getSource()).setBytesPerMessage(bytesPerMessage);
	}

	public void setShouldTruncate(boolean shouldTruncate) {
		((ByteStreamSource) this.getSource()).setShouldTruncate(shouldTruncate);
	}

}
