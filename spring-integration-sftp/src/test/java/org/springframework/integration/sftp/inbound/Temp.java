package org.springframework.integration.sftp.inbound;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class Temp {

	public static void main(String[] args) throws Exception {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		
		//client.retrieveFile("path", outputStream);
		
		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

		System.out.println(inputStream.read());
		System.out.println(inputStream.read());
		System.out.println(inputStream.read());
	}
}
