/*
 * Copyright 2014-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.test.mail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ServerSocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Base64Utils;

/**
 * A basic test mail server for pop3, imap,
 * Serves up a canned email message with each protocol.
 * For smtp, it handles the basic handshaking and captures
 * the pertinent data so it can be verified by a test case.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public final class TestMailServer {

	public static SmtpServer smtp(int port) {
		try {
			return new SmtpServer(port);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static Pop3Server pop3(int port) {
		try {
			return new Pop3Server(port);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static ImapServer imap(int port) {
		try {
			return new ImapServer(port);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static class SmtpServer extends MailServer {

		SmtpServer(int port) throws IOException {
			super(port);
		}

		@Override
		protected MailHandler mailHandler(Socket socket) {
			return new SmtpHandler(socket);
		}

		class SmtpHandler extends MailHandler {

			SmtpHandler(Socket socket) {
				super(socket);
			}

			@Override // NOSONAR
			void doRun() { // NOSONAR
				try {
					write("220 foo SMTP");
					while (!socket.isClosed()) {
						String line = reader.readLine();
						if (line == null) {
							break;
						}
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
					if (!this.stopped) {
						LOGGER.error(IO_EXCEPTION, e);
					}
				}
			}

		}

	}

	public static class Pop3Server extends MailServer {

		Pop3Server(int port) throws IOException {
			super(port);
		}

		@Override
		protected MailHandler mailHandler(Socket socket) {
			return new Pop3Handler(socket);
		}

		class Pop3Handler extends MailHandler {

			private static final String PLUS_OK = "+OK";

			Pop3Handler(Socket socket) {
				super(socket);
			}

			@Override // NOSONAR
			void doRun() {
				try {
					write("+OK POP3");
					while (!socket.isClosed()) {
						String line = reader.readLine();
						if (line == null) {
							break;
						}
						switch (line) {
							case "CAPA":
								write(PLUS_OK);
								write("USER");
								write(".");
								break;
							case "USER user":
							case "PASS pw":
							case "NOOP":
								write(PLUS_OK);
								break;
							case "STAT":
								write("+OK 1 3");
								break;
							case "RETR 1":
								write(PLUS_OK);
								write(MESSAGE);
								write(".");
								break;
							case "QUIT":
								write(PLUS_OK);
								socket.close();
								break;
							default:
								throw new UnsupportedOperationException(line);
						}
					}
				}
				catch (IOException e) {
					if (!this.stopped) {
						LOGGER.error(IO_EXCEPTION, e);
					}
				}
			}

		}

	}

	public static class ImapServer extends MailServer {

		private volatile boolean seen;

		private volatile boolean idled;

		ImapServer(int port) throws IOException {
			super(port);
		}

		@Override
		public void resetServer() {
			super.resetServer();
			this.seen = false;
			this.idled = false;
		}

		@Override
		protected MailHandler mailHandler(Socket socket) {
			return new ImapHandler(socket);
		}

		class ImapHandler extends MailHandler {

			private static final String OK_FETCH_COMPLETED = "OK FETCH completed";

			/**
			 * Time to wait while IDLE before returning a result.
			 */
			private static final int IDLE_WAIT_TIME = 500;

			ImapHandler(Socket socket) {
				super(socket);
			}

			@Override // NOSONAR
			void doRun() {
				try {
					write("* OK IMAP4rev1 Service Ready");
					String idleTag = "";
					while (!socket.isClosed()) {
						String line = reader.readLine();
						if (line == null) {
							break;
						}
						String tag = line.substring(0, line.indexOf(' ') + 1);
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
							write("* OK [PERMANENTFLAGS (\\Deleted \\Seen \\*)]"); // \* - user flags allowed
							write(tag + "OK SELECT completed");
						}
						else if (line.endsWith("EXAMINE INBOX")) {
							write(tag + "OK");
						}
						else if (line.endsWith("SEARCH FROM bar@baz UNSEEN ALL")) {
							searchReply(tag);
						}
						else if (line.endsWith("SEARCH NOT (DELETED) NOT (SEEN) NOT (KEYWORD testSIUserFlag) ALL")) {
							searchReply(tag);
							assertions.add("searchWithUserFlag");
						}
						else if (line.contains("FETCH 1 (ENVELOPE")) {
							write("* 1 FETCH (RFC822.SIZE "
									+ MESSAGE.length()
									+ " INTERNALDATE \"27-May-2013 09:45:41 +0000\" "
									+ "FLAGS (\\Seen) "
									+ "ENVELOPE (\"Mon, 27 May 2013 15:14:49 +0530\" "
									+ "\"Test Email\" "
									+ "((\"Bar\" NIL \"bar\" \"baz\")) " // From
									+ "((\"Bar\" NIL \"bar\" \"baz\")) " // Sender
									+ "((\"Bar\" NIL \"bar\" \"baz\")) " // Reply To
									+ "((\"Foo\" NIL \"foo\" \"bar\")) " // To
									+ "((NIL NIL \"a\" \"b\") (NIL NIL \"c\" \"d\")) " // cc
									+ "((NIL NIL \"e\" \"f\") (NIL NIL \"g\" \"h\")) " // bcc
									+ "\"<4DA0A7E4.3010506@baz.net>\" " // In reply to
									+ "\"<CACVnpJkAUUfa3d_-4GNZW2WpxbB39tBCHC=T0gc7hty6dOEHcA@foo.bar.com>\") " // msgid
									+ "BODYSTRUCTURE "
									+ "(\"TEXT\" \"PLAIN\" (\"CHARSET\" \"ISO-8859-1\") NIL NIL \"7BIT\" 1 5)))");
							write(tag + OK_FETCH_COMPLETED);
						}
						else if (line.contains("FETCH 2 (BODYSTRUCTURE)")) {
							write("* 2 FETCH " +
									"BODYSTRUCTURE "
									+ "(\"TEXT\" \"PLAIN\" (\"CHARSET\" \"ISO-8859-1\") NIL NIL \"7BIT\" 1 5)))");
							write(tag + OK_FETCH_COMPLETED);
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
							write(tag + OK_FETCH_COMPLETED);
						}
						else if (line.contains("FETCH 1 (BODY.PEEK")) {
							write("* 1 FETCH (BODY[]<0> {" + (MESSAGE.length() + 2) + "}");
							write(MESSAGE);
							write(")");
							write(tag + OK_FETCH_COMPLETED);
						}
						else if (line.contains("CLOSE")) {
							write(tag + "OK CLOSE completed");
						}
						else if (line.contains("NOOP")) {
							write(tag + "OK NOOP completed");
						}
						else if (line.endsWith("STORE 1 +FLAGS (testSIUserFlag)")) {
							write(tag + "OK STORE completed");
							assertions.add("storeUserFlag");
						}
						else if (line.endsWith("IDLE")) {
							write("+ idling");
							idleTag = tag;
							if (!idled) {
								try {
									Thread.sleep(IDLE_WAIT_TIME);
									write("* 2 EXISTS");
									seen = false;
								}
								catch (@SuppressWarnings("unused") InterruptedException e) {
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
					if (!this.stopped) {
						LOGGER.error(IO_EXCEPTION, e);
					}
				}
			}

			void searchReply(String tag) throws IOException {
				if (seen) {
					write("* SEARCH");
				}
				else {
					write("* SEARCH 1");
				}
				write(tag + "OK SEARCH completed");
			}

		}

	}

	public abstract static class MailServer implements Runnable {

		protected final Log LOGGER = LogFactory.getLog(getClass()); // NOSONAR

		protected static final String IO_EXCEPTION = "IOException"; // NOSONAR

		private final ServerSocket serverSocket;

		private final ExecutorService exec = Executors.newCachedThreadPool();

		protected final Set<String> assertions = new HashSet<>(); // NOSONAR protected

		protected final List<String> messages = new ArrayList<>(); // NOSONAR protected

		private final List<MailHandler> handlers = new ArrayList<>();

		private volatile boolean listening;

		MailServer(int port) throws IOException {
			this.serverSocket = ServerSocketFactory.getDefault().createServerSocket(port);
			this.listening = true;
			exec.execute(this);
		}

		public int getPort() {
			return this.serverSocket.getLocalPort();
		}

		public boolean isListening() {
			return listening;
		}

		public List<String> getMessages() {
			return messages;
		}

		public void resetServer() {
			this.assertions.clear();
		}

		public boolean assertReceived(String assertion) {
			return this.assertions.contains(assertion);
		}

		@Override
		public void run() {
			try {
				while (!serverSocket.isClosed()) {
					Socket socket = this.serverSocket.accept();
					MailHandler mailHandler = mailHandler(socket);
					this.handlers.add(mailHandler);
					exec.execute(mailHandler);
				}
			}
			catch (@SuppressWarnings("unused") IOException e) {
				this.listening = false;
			}
		}

		protected abstract MailHandler mailHandler(Socket socket);

		public void stop() {
			try {
				for (MailHandler handler : this.handlers) {
					handler.stop();
				}
				this.serverSocket.close();
			}
			catch (IOException e) {
				LOGGER.error(IO_EXCEPTION, e);
			}
			this.exec.shutdownNow();
		}

		public abstract class MailHandler implements Runnable {

			public static final String BODY = "foo\r\n";

			public static final String MESSAGE =
					"To: Foo <foo@bar>\r\n"
							+ "cc: a@b, c@d\r\n"
							+ "bcc: e@f, g@h\r\n"
							+ "From: Bar <bar@baz>, Bar2 <bar2@baz>\r\n"
							+ "Subject: Test Email\r\n"
							+ "\r\n" + BODY;

			protected final Socket socket; // NOSONAR protected

			private BufferedWriter writer;

			protected StringBuilder sb = new StringBuilder(); // NOSONAR protected

			protected BufferedReader reader; // NOSONAR protected

			protected boolean stopped; // NOSONAR

			MailHandler(Socket socket) {
				this.socket = socket;
			}

			@Override
			public void run() {
				try {
					this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
					this.writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
				}
				catch (IOException e) {
					LOGGER.error(IO_EXCEPTION, e);
				}
				doRun();
			}

			protected void write(String str) throws IOException {
				this.writer.write(str);
				this.writer.write("\r\n");
				this.writer.flush();
			}

			abstract void doRun();

			void stop() {
				this.stopped = true;
				try {
					this.socket.close();
				}
				catch (IOException e) {
					// NOSONAR
				}
			}

		}

	}

	private TestMailServer() {
	}

}
