package org.springframework.integration.adapter.ftp;

import java.io.IOException;
import java.net.SocketException;

import org.apache.commons.net.ftp.FTPClient;

public interface FTPClientFactory {

	FTPClient getClient() throws SocketException, IOException;

}
