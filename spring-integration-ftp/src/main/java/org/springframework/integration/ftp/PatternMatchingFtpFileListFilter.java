package org.springframework.integration.ftp;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;


/**
 * Validates {@link org.apache.commons.net.ftp.FTPFile}s against a {@link java.util.regex.Pattern}.
 * Patterned very much like {@link org.springframework.integration.file.PatternMatchingFileListFilter}.
 *
 * @author Josh Long
 */
public class PatternMatchingFtpFileListFilter extends AbstractFtpFileListFilter implements InitializingBean {

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
    public boolean accept(FTPFile ftpFile) {
        if (logger.isDebugEnabled()) {
            logger.debug("testing: " + ToStringBuilder.reflectionToString(ftpFile));
        }

        return (ftpFile != null) && this.pattern.matcher(ftpFile.getName()).matches();
    }

    public void afterPropertiesSet() throws Exception {
        if (StringUtils.hasText(this.patternExpression) && (this.pattern == null)) {
            this.pattern = Pattern.compile(this.patternExpression);
        }

        Assert.notNull(this.pattern, "the pattern must not be null");
    }
}
