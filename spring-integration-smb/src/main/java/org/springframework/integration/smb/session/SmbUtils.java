/**
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.smb.session;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public abstract class SmbUtils {

	private SmbUtils() {
	}

	/**
     * Read the specified file into a byte array.
     * @param _file file
     * @return byte array of file contents
     * @throws IOException
     */
    public static byte[] readFile(File _file) throws IOException {
    	FileInputStream stream = new FileInputStream(_file);
    	try {
    		FileChannel fc = stream.getChannel();
    		MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
    		return bb.array();
    
    		/* Instead of using default, pass in a decoder. */
    		// return Charset.defaultCharset().decode(bb).toString();
    
    	} finally {
    		stream.close();
    	}
    }

}
