/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.integration.mail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ServerSocketFactory;

import org.springframework.util.Base64Utils;

/**
 * @author Gary Russell
 *
 */
public class PoorMansMailServer {

	public static SmtpServer smtp(int port) {
		try {
			return new SmtpServer(port);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Pop3Server pop3(int port) {
		try {
			return new Pop3Server(port);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static ImapServer imap(int port) {
		try {
			return new ImapServer(port);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static class SmtpServer extends MailServer {

		public SmtpServer(int port) throws IOException {
			super(port);
		}

		@Override
		protected MailHandler mailHandler(Socket socket) {
			return new SmtpHandler(socket);
		}

		public class SmtpHandler extends MailHandler {

			public SmtpHandler(Socket socket) {
				super(socket);
			}

			@Override
			void doRun() {
				try {
					write("220 foo SMTP");
					while (!socket.isClosed()) {
						String line = reader.readLine();
						if (line.contains("EHLO")) {
							write("250-foo hello [0,0,0,0], foo");
							write("250-AUTH LOGIN PLAIN");
							write("250 OK");
						}
						else if (line.contains("MAIL FROM")) {
							write("250 OK");
						}
						else if (line.contains("RCPT TO")) {
							write("250 OK");
						}
						else if (line.contains("AUTH LOGIN")) {
							write("334 VXNlcm5hbWU6");
						}
						else if (line.contains("dXNlcg==")) { // base64 'user'
							sb.append("user:");
							sb.append((new String(Base64Utils.decode(line.getBytes()))));
							sb.append("\n");
							write("334 UGFzc3dvcmQ6");
						}
						else if (line.contains("cHc=")) { // base64 'pw'
							sb.append("password:");
							sb.append((new String(Base64Utils.decode(line.getBytes()))));
							sb.append("\n");
							write("235");
						}
						else if (line.equals("DATA")) {
							write("354");
						}
						else if (line.equals(".")) {
							write("250");
						}
						else if (line.equals("QUIT")) {
							write("221");
							socket.close();
						}
						else {
							sb.append(line);
							sb.append("\n");
						}
					}
					messages.add(sb.toString());
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

	}

	public static class Pop3Server extends MailServer {

		public Pop3Server(int port) throws IOException {
			super(port);
		}

		@Override
		protected MailHandler mailHandler(Socket socket) {
			return new Pop3Handler(socket);
		}

		public class Pop3Handler extends MailHandler {

			public Pop3Handler(Socket socket) {
				super(socket);
			}

			@Override
			void doRun() {
				try {
					write("+OK POP3");
					while (!socket.isClosed()) {
						String line = reader.readLine();
						if ("CAPA".equals(line)) {
							write("+OK");
							write("USER");
							write(".");
						}
						else if ("USER user".equals(line)) {
							write("+OK");
						}
						else if ("PASS pw".equals(line)) {
							write("+OK");
						}
						else if ("STAT".equals(line)) {
							write("+OK 1 3");
						}
						else if ("NOOP".equals(line)) {
							write("+OK");
						}
						else if ("RETR 1".equals(line)) {
							write("+OK");
							write(MESSAGE);
							write(".");
						}
						else if ("QUIT".equals(line)) {
							write("+OK");
							socket.close();
						}
					}
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

	}

	public static class ImapServer extends MailServer {

		private boolean seen;

		private boolean idled;

		public ImapServer(int port) throws IOException {
			super(port);
		}

		@Override
		protected MailHandler mailHandler(Socket socket) {
			return new ImapHandler(socket);
		}

		public class ImapHandler extends MailHandler {

			public ImapHandler(Socket socket) {
				super(socket);
			}

			@Override
			void doRun() {
				try {
					write("* OK IMAP4rev1 Service Ready");
					String idleTag = "";
					while (!socket.isClosed()) {
						String line = reader.readLine();
						if (line == null) {
							break;
						}
						String tag = line.substring(0, line.indexOf(" ") + 1);
						if (line.endsWith("CAPABILITY")) {
							write("* CAPABILITY IDLE IMAP4rev1");
							write(tag + "OK CAPABILITY completed");
						}
						else if (line.endsWith("LOGIN user pw")) {
							write(tag + "OK LOGIN completed");
						}
						else if (line.endsWith("LIST \"\" INBOX")) {
							write("* LIST \"/\" \"INBOX\"");
							write(tag + "OK LIST completed");
						}
						else if (line.endsWith("LIST \"\" \"\"")) {
							write("* LIST \"/\" \"\"");
							write(tag + "OK LIST completed");
						}
						else if (line.endsWith("SELECT INBOX")) {
							write("* 1 EXISTS");
							if (!seen) {
								write("* 1 RECENT");
								write("* OK [UNSEEN 1]");
							}
							else {
								write("* OK");
							}
							write(tag + "OK SELECT completed");
						}
						else if (line.endsWith("EXAMINE INBOX")) {
							write(tag + "OK");
						}
						else if (line.endsWith("SEARCH FROM bar@baz UNSEEN ALL")) {
							if (seen) {
								write("* SEARCH");
							}
							else {
								write("* SEARCH 1");
							}
							write(tag + "OK SEARCH completed");
						}
						else if (line.contains("FETCH 1 (ENVELOPE")) {
							write("* 1 FETCH (RFC822.SIZE 6909 INTERNALDATE \"27-May-2013 09:45:41 +0000\" "
									+ "FLAGS (\\Seen) "
									+ "ENVELOPE (\"Mon, 27 May 2013 15:14:49 +0530\" "
									+ "\"Test Email\" ((\"Foo\" NIL \"foo\" \"bar.tv\")) "
									+ "((\"Foo\" NIL \"foo\" \"bar.tv\")) "
									+ "((\"Foo\" NIL \"foo\" \"bar.tv\")) "
									+ "((\"Bar\" NIL \"bar\" \"baz.net\")) NIL NIL "
									+ "\"<4DA0A7E4.3010506@baz.net>\" "
									+ "\"<CACVnpJkAUUfa3d_-4GNZW2WpxbB39tBCHC=T0gc7hty6dOEHcA@foo.bar.com>\") "
									+ "BODYSTRUCTURE (\"TEXT\" \"PLAIN\" (\"CHARSET\" \"ISO-8859-1\") NIL NIL \"7BIT\" 1176 43)))");
							write(tag + "OK FETCH completed");
						}
						else if (line.contains("FETCH 2 (BODYSTRUCTURE)")) {
							write("* 2 FETCH " +
									"BODYSTRUCTURE (\"TEXT\" \"PLAIN\" (\"CHARSET\" \"ISO-8859-1\") NIL NIL \"7BIT\" 1176 43)))");
							write(tag + "OK FETCH completed");
						}
						else if (line.contains("STORE 1 +FLAGS (\\Flagged)")) {
							write("* 1 FETCH (FLAGS (\\Flagged))");
							write(tag + "OK STORE completed");
						}
						else if (line.contains("STORE 1 +FLAGS (\\Seen)")) {
							write("* 1 FETCH (FLAGS (\\Flagged \\Seen))");
							write(tag + "OK STORE completed");
							seen = true;
						}
						else if (line.contains("FETCH 1 FLAGS")) {
							write("* 1 FLAGS(\\Seen)");
							write(tag + "OK FETCH completed");
						}
						else if (line.contains("FETCH 1 (BODY.PEEK")) {
							write("* 1 FETCH (BODY[]<0> {" + (MESSAGE.length() + 2) + "}");
							write(MESSAGE);
							write(")");
							write(tag + "OK FETCH completed");
						}
						else if (line.contains("CLOSE")) {
							write(tag + "OK CLOSE completed");
						}
						else if (line.contains("NOOP")) {
							write(tag + "OK NOOP completed");
						}
						else if (line.endsWith("IDLE")) {
							write("+ idling");
							idleTag = tag;
							if (!idled) {
								try {
									Thread.sleep(3000);
									write("* 2 EXISTS");
									seen = false;
								}
								catch (InterruptedException e) {
									Thread.currentThread().interrupt();
								}
							}
							idled = true;
						}
						else if (line.equals("DONE")) {
							write(idleTag + "OK");
						}
						else if (line.contains("LOGOUT")) {
							write(tag + "OK LOGOUT completed");
							this.socket.close();
						}
					}
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

	}

	public abstract static class MailServer implements Runnable {

		private final ServerSocket socket;

		private final ExecutorService exec = Executors.newCachedThreadPool();

		protected final List<String> messages = new ArrayList<String>();

		private volatile boolean listening;

		public MailServer(int port) throws IOException {
			this.socket = ServerSocketFactory.getDefault().createServerSocket(port);
			this.listening = true;
			exec.execute(this);
		}

		public boolean isListening() {
			return listening;
		}

		public List<String> getMessages() {
			return messages;
		}

		@Override
		public void run() {
			try {
				while (!socket.isClosed()) {
					Socket socket = this.socket.accept();
					exec.execute(mailHandler(socket));
				}
			}
			catch (IOException e) {
				this.listening = false;
			}
		}

		protected abstract MailHandler mailHandler(Socket socket);

		public void stop() {
			try {
				this.socket.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			this.exec.shutdownNow();
		}

		public abstract class MailHandler implements Runnable {

			protected static final String MESSAGE = "To: foo@bar\r\nFrom: bar@baz\r\nSubject: Test Email\r\n\r\nfoo";

			protected final Socket socket;

			private BufferedWriter writer;

			protected StringBuilder sb = new StringBuilder();

			protected BufferedReader reader;

			public MailHandler(Socket socket) {
				this.socket = socket;
			}

			@Override
			public void run() {
				try {
					this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
					this.writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				doRun();
			}

			protected void write(String str) throws IOException {
				this.writer.write(str);
				this.writer.write("\r\n");
				this.writer.flush();
			}

			abstract void doRun();

		}

	}

	private PoorMansMailServer() {
	}

}
