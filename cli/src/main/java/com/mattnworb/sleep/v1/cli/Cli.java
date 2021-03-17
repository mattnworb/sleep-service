package com.mattnworb.sleep.v1.cli;

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.google.protobuf.TextFormat;
import com.mattnworb.sleep.v1.SleepRequest;
import com.mattnworb.sleep.v1.SleepResponse;
import com.mattnworb.sleep.v1.SleepServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cli {
  private static final Logger log = LoggerFactory.getLogger(Cli.class);

  @SuppressWarnings("unused")
  private final PrintStream out;

  private final PrintStream err;

  public Cli(final PrintStream out, final PrintStream err) {
    this.out = out;
    this.err = err;
  }

  int run(final String... args) {
    ArgumentParser parser = createParser();

    Namespace ns;
    try {
      ns = parser.parseArgs(args);
    } catch (ArgumentParserException e) {
      @SuppressWarnings("DefaultCharset")
      PrintWriter writer = new PrintWriter(err);
      parser.handleError(e, writer);
      return 1;
    }

    setupLogging(ns.getInt("logging-verbosity"));

    sendBlockingRequest(ns);

    return 0;
  }

  private ArgumentParser createParser() {
    ArgumentParser parser = ArgumentParsers.newFor("sleep-cli").build();

    parser
        .addArgument("-v")
        .action(Arguments.count())
        .dest("logging-verbosity")
        .help("set logging verbosity")
        .setDefault(1);

    // Connection related settings
    parser
        .addArgument("target")
        .metavar("TARGET")
        .help("gRPC target of the sleep-service to send requests to.");

    parser
        .addArgument("sleepTime")
        .metavar("SLEEP_TIME_MILLIS")
        .help("value to send in request")
        .type(Integer.class);

    parser
        .addArgument("--deadline")
        .setDefault(1000)
        .type(Integer.class)
        .help("Deadline value to set in gRPC requests in milliseconds");

    return parser;
  }

  /**
   * Configure logback based on the given log level. Resets the logback LoggerContext so that any
   * configuration that logback automatically loaded is discarded.
   */
  private static void setupLogging(int logLevel) {
    final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    lc.reset();

    // map 0 (the default) to WARN, and max out at ALL
    final Level[] levels = new Level[] {Level.WARN, Level.INFO, Level.DEBUG, Level.ALL};
    final Level level = levels[Math.min(logLevel, levels.length - 1)];

    final ch.qos.logback.classic.Logger rootLogger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(level);

    // TODO (mattbrown 4/28/20) wire up the ConsoleAppender with this.out or this.err

    final BasicConfigurator configurator = new BasicConfigurator();
    configurator.setContext(lc);
    configurator.configure(lc);
  }

  private void sendBlockingRequest(final Namespace ns) {
    ManagedChannel channel =
        ManagedChannelBuilder.forTarget(ns.getString("target"))
            .usePlaintext()
            .intercept(new LoggingClientInterceptor())
            .build();

    int deadlineMillis = ns.getInt("deadline");
    int sleepTime = ns.getInt("sleepTime");

    SleepRequest request = SleepRequest.newBuilder().setSleepTimeMillis(sleepTime).build();

    log.info("sending request: {}", TextFormat.shortDebugString(request));

    try {
      SleepResponse response =
          SleepServiceGrpc.newBlockingStub(channel)
              .withDeadlineAfter(deadlineMillis, TimeUnit.MILLISECONDS)
              .sleep(request);

      log.info("received response: {}", TextFormat.shortDebugString(response));
    } catch (StatusRuntimeException ex) {
      log.warn("caught StatusRuntimeException", ex);
      if (ex.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
        log.info("caught DEADLINE_EXCEEDED, sleeping to see if a response comes in");
        try {
          Thread.sleep(10000);
        } catch (InterruptedException ignored) {
        }
      }
    }
  }
}
