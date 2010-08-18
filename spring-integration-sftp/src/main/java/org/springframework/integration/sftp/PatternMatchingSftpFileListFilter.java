/*
 * Copyright 2010 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.sftp;

import com.jcraft.jsch.ChannelSftp;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;


/**
 * Validates {@link com.jcraft.jsch.ChannelSftp.LsEntry}s against a {@link java.util.regex.Pattern}.
 * Patterned very much like {@link org.springframework.integration.file.PatternMatchingFileListFilter}.
 *
 * @author Josh Long
 */
public class PatternMatchingSftpFileListFilter extends AbstractSftpFileListFilter implements InitializingBean {

    private Log logger = LogFactory.getLog(getClass());
    
    private Pattern pattern;
    private String patternExpression;

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public void setPatternExpression(String patternExpression) {
        this.patternExpression = patternExpression;
    }

    @Override
    public boolean accept(ChannelSftp.LsEntry lsEntry) {
        if (logger.isDebugEnabled()) {
            logger.debug("testing: " + ToStringBuilder.reflectionToString(lsEntry));
        }

        return (lsEntry != null) && this.pattern.matcher(lsEntry.getFilename()).matches();
    }

    public void afterPropertiesSet() throws Exception {
        if (StringUtils.hasText(this.patternExpression) && (this.pattern == null)) {
            this.pattern = Pattern.compile(this.patternExpression);
        }

        Assert.notNull(this.pattern, "the pattern must not be null");
    }
}
