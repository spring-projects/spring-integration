/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.file.entries;

/**
 * Responsible for coercing a String identification out of the T entry.
 * @param <T>        the type of entry (there's an implementation for FTP, SFTP, and plain-old java.io.Files)
 *
 * @author Josh Long
 */
public interface EntryNamer<T> {

    /**
     * This is the one place I couldn't spackle over the interface differences between an FTPFile (FTP adapter), File (File adapter), and LsEntry (SFTP adapter)
     * with generics alone. So we have a typed strategy implementation for accessing a property ....
     *
     *
     * @param entry  the entry in a file system listing
     * @return the String name that might be used to reference that entry or to do regular expression checks against
     */
    String nameOf(T entry);
}
