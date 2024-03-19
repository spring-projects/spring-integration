/*
 * Copyright 2016-2024 the original author or authors.
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
package org.springframework.integration.nats;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to run gnatds for tests. Highly based on the 1.0 client's NatsServer code.
 *
 * <p>This class will be removed once we move to testContainer approach
 *
 * <p>More info here: https://escmconfluence.1dc.com/display/IPG/NATS+-+Integration+Testing+Approach
 */

/**
 * @author Viktor Rohlenko - lead and architect
 * @author Vennila Pazhamalai - maintainer
 * @author Vivek Duraisamy - maintainer
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 * @since 6.4.x
 */
public class NatsLocalTestServer implements AutoCloseable {

  /* Default location of server installation (deployment type without docker).
   * This can be overwritten by e.g. using -Dnats_server_path={your local installation}
   */
  private static final String NATS_SERVER =
      "C:\\Software\\nats\\nats-server-v2.6.6-windows-386\\nats-server";

  // Use a new port each time, we increment and get so start at the normal
  // port
  private static final AtomicInteger portCounter = new AtomicInteger(5222);

  private final int port;

  private final boolean debug;

  private String configFilePath;

  private Process process;

  private String cmdLine;

  private String[] customArgs;

  private String[] configInserts;

  public NatsLocalTestServer() {
    this(false);
  }

  public NatsLocalTestServer(final boolean pDebug) {
    this(NatsLocalTestServer.nextPort(), pDebug);
  }

  public NatsLocalTestServer(final int portNo, final boolean pDebug) {
    this.port = portNo;
    this.debug = pDebug;
    start();
  }

  public NatsLocalTestServer(final String pConfigFilePath, final boolean pDebug) {
    this.configFilePath = pConfigFilePath;
    this.debug = pDebug;
    this.port = nextPort();
    start();
  }

  public NatsLocalTestServer(
      final String pConfigFilePath,
      final String[] pInserts,
      final int portNo,
      final boolean pDebug) {
    this.configFilePath = pConfigFilePath;
    this.configInserts = pInserts;
    this.debug = pDebug;
    this.port = portNo;
    start();
  }

  public NatsLocalTestServer(final String pConfigFilePath, final int portNo, final boolean pDebug) {
    this.configFilePath = pConfigFilePath;
    this.debug = pDebug;
    this.port = portNo;
    start();
  }

  public NatsLocalTestServer(final String[] pCustomArgs, final boolean pDebug) {
    this.port = NatsLocalTestServer.nextPort();
    this.debug = pDebug;
    this.customArgs = pCustomArgs;
    start();
  }

  public NatsLocalTestServer(final String[] pCustomArgs, final int portNo, final boolean pDebug) {
    this.port = portNo;
    this.debug = pDebug;
    this.customArgs = pCustomArgs;
    start();
  }

  public static String generateNatsServerVersionString() {
    final ArrayList<String> cmd = new ArrayList<>();

    String server_path = System.getProperty("nats_server_path");

    if (server_path == null) {
      server_path = NatsLocalTestServer.NATS_SERVER;
    }

    cmd.add(server_path);
    cmd.add("--version");

    try {
      final ProcessBuilder pb = new ProcessBuilder(cmd);
      final Process process = pb.start();
      process.waitFor();
      final BufferedReader reader =
          new BufferedReader(new InputStreamReader(process.getInputStream()));
      final ArrayList<String> lines = new ArrayList<>();
      String line = "";
      while ((line = reader.readLine()) != null) {
        lines.add(line);
      }

      if (!lines.isEmpty()) {
        return lines.get(0);
      }

      return null;
    } catch (final Exception exp) {
      return null;
    }
  }

  public static int nextPort() {
    return NatsLocalTestServer.portCounter.incrementAndGet();
  }

  public static int currentPort() {
    return NatsLocalTestServer.portCounter.get();
  }

  public static String getURIForPort(final int port) {
    return "nats://localhost:" + port;
  }

  public void start() {
    final ArrayList<String> cmd = new ArrayList<>();

    String server_path = System.getProperty("nats_server_path");

    if (server_path == null) {
      server_path = NatsLocalTestServer.NATS_SERVER;
    }

    cmd.add(server_path);

    // Rewrite the port to a new one, so we don't reuse the same one over
    // and over
    if (this.configFilePath != null) {
      final Pattern pattern = Pattern.compile("port: (\\d+)");
      final Matcher matcher = pattern.matcher("");
      BufferedReader read = null;
      File tmp = null;
      BufferedWriter write = null;
      String line;

      try {
        tmp = File.createTempFile("nats_java_test", ".conf");
        write = new BufferedWriter(new FileWriter(tmp));
        read = new BufferedReader(new FileReader(this.configFilePath));

        while ((line = read.readLine()) != null) {
          matcher.reset(line);

          if (matcher.find()) {
            line = line.replace(matcher.group(1), String.valueOf(this.port));
          }

          write.write(line);
          write.write("\n");
        }

        if (this.configInserts != null) {
          for (final String s : this.configInserts) {
            write.write(s);
            write.write("\n");
          }
        }
      } catch (final Exception exp) {
        System.out.println("%%% Error parsing config file for port.");
        return;
      } finally {
        if (read != null) {
          try {
            read.close();
          } catch (final Exception e) {
            throw new IllegalStateException("Failed to read config file");
          }
        }
        if (write != null) {
          try {
            write.close();
          } catch (final Exception e) {
            throw new IllegalStateException("Failed to update config file");
          }
        }
      }

      cmd.add("--config");
      cmd.add(tmp.getAbsolutePath());
    } else {
      cmd.add("--port");
      cmd.add(String.valueOf(this.port));
    }

    if (this.customArgs != null) {
      cmd.addAll(Arrays.asList(this.customArgs));
    }

    if (this.debug) {
      cmd.add("-DV");
    }

    this.cmdLine = String.join(" ", cmd);

    try {
      final ProcessBuilder pb = new ProcessBuilder(cmd);

      if (this.debug) {
        System.out.println("%%% Starting [" + this.cmdLine + "] with redirected IO");
        pb.inheritIO();
      } else {
        String errFile = null;
        final String osName = System.getProperty("os.name");

        if (osName != null && osName.contains("Windows")) {
          // Windows uses the "nul" file.
          errFile = "nul";
        } else {
          errFile = "/dev/null";
        }

        pb.redirectError(new File(errFile));
      }

      this.process = pb.start();

      int tries = 10;
      // wait at least 1x and maybe 10
      do {
        try {
          Thread.sleep(100);
        } catch (final Exception exp) {
          // Give the server time to get going
        }
        tries--;
      } while (!this.process.isAlive() && tries > 0);

      System.out.println("%%% Started [" + this.cmdLine + "]");
    } catch (final IOException ex) {
      System.out.println("%%% Failed to start [" + this.cmdLine + "] with message:");
      System.out.println("\t" + ex.getMessage());
      System.out.println("%%% Make sure that the nats-server is installed and in your PATH.");
      System.out.println(
          "%%% See https://github.com/nats-io/nats-server for information on installation");

      throw new IllegalStateException("Failed to run [" + this.cmdLine + "]");
    }
  }

  public int getPort() {
    return this.port;
  }

  public String getURI() {
    return getURIForPort(this.port);
  }

  public void shutdown() {

    if (this.process == null) {
      return;
    }

    this.process.destroy();

    System.out.println("%%% Shut down [" + this.cmdLine + "]");

    this.process = null;
  }

  /** Synonomous with shutdown. */
  @Override
  public void close() {
    shutdown();
  }
}
