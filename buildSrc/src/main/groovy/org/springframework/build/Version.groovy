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

package org.springframework.build

import org.gradle.api.InvalidUserDataException


class Version {
    /**
     * Indicates whether a version is a release, milestone, or snapshot
     */
    static final String RELEASE = 'RELEASE'
    static final String MILESTONE = 'MILESTONE'
    static final String SNAPSHOT = 'SNAPSHOT'

    String value
    String releaseType
    int majorVersion
    int minorVersion

    /**
     * @param dotted -quad version spec, e.g.: 1.0.0.RELEASE
     */
    public Version(String value) {
        this.value = value;
        this.releaseType = releaseTypeFor(value);
        this.majorVersion = Integer.parseInt(this.value.substring(0, this.value.indexOf('.')));
        String afterMajor = this.value.substring(this.value.indexOf('.') + 1);
        this.minorVersion = Integer.parseInt(afterMajor.substring(0, afterMajor.indexOf('.')));
    }

    public int getMajorVersion() {
        return this.majorVersion;
    }

    public int getMinorVersion() {
        return this.minorVersion
    }

    /**
     * @return 1.0.x style
     */
    public String getWildcardValue() {
        return majorVersion + '.' + minorVersion + '.x';
    }

    /**
     * @return the version string returned by   {@link getValue ( )}
     */
    public String toString() {
        return this.getValue();
    }

    /**
     * @param dotted -quad version spec, e.g.: 1.0.0.RELEASE
     */
    public static String releaseTypeFor(String version) {
        if (version.endsWith("RELEASE")) return RELEASE;
        if (version.endsWith("SNAPSHOT")) return SNAPSHOT;
        if (version.matches(".*\\.M[0-9]+\$")) return MILESTONE;
        if (version.matches(".*\\.RC[0-9]+\$")) return MILESTONE;

        throw new InvalidUserDataException("unknown version scheme: " +
                "versions must end in (SNAPSHOT|M[0-9]+|RC[0-9]+|RELEASE), " +
                "but got (" + version + ")");
    }
}
