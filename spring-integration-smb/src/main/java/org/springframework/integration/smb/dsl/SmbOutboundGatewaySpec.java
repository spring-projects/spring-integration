/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.smb.dsl;

import org.codelibs.jcifs.smb.impl.SmbFile;

import org.springframework.integration.file.dsl.RemoteFileOutboundGatewaySpec;
import org.springframework.integration.smb.filters.SmbRegexPatternFileListFilter;
import org.springframework.integration.smb.filters.SmbSimplePatternFileListFilter;
import org.springframework.integration.smb.outbound.SmbOutboundGateway;

/**
 * A {@link RemoteFileOutboundGatewaySpec} for SMB.
 *
 * @author Gregory Bragg
 * @author Daniel Frey
 *
 * @since 6.0
 */
public class SmbOutboundGatewaySpec extends RemoteFileOutboundGatewaySpec<SmbFile, SmbOutboundGatewaySpec> {

	protected SmbOutboundGatewaySpec(SmbOutboundGateway outboundGateway) {
		super(outboundGateway);
	}

	/**
	 * @see SmbSimplePatternFileListFilter
	 */
	@Override
	public SmbOutboundGatewaySpec patternFileNameFilter(String pattern) {
		return filter(new SmbSimplePatternFileListFilter(pattern));
	}

	/**
	 * @see SmbRegexPatternFileListFilter
	 */
	@Override
	public SmbOutboundGatewaySpec regexFileNameFilter(String regex) {
		return filter(new SmbRegexPatternFileListFilter(regex));
	}

}
