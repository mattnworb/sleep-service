package com.mattnworb.sleep.v1.server;

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.services.HealthStatusManager;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
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

    return startServer(ns.getInt("port"));
  }

  private ArgumentParser createParser() {
    ArgumentParser parser = ArgumentParsers.newFor("sleep-service").build();

    parser
        .addArgument("-v")
        .action(Arguments.count())
        .dest("logging-verbosity")
        .help("set logging verbosity")
        .setDefault(1);

    parser.addArgument("--port").type(Integer.class).help("port to listen on").setDefault(5000);

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

    final BasicConfigurator configurator = new BasicConfigurator();
    configurator.setContext(lc);
    configurator.configure(lc);
  }

  private int startServer(final int port) {
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("sleep-service-scheduler-%%d").build();
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5, threadFactory);

    HealthStatusManager healthStatusManager = new HealthStatusManager();

    Server server =
        NettyServerBuilder.forPort(port)
            .addService(healthStatusManager.getHealthService())
            .addService(ProtoReflectionService.newInstance())
            .addService(new SleepServiceImpl(scheduler))
            .intercept(new LoggingServerInterceptor())
            .build();

    try {
      server.start();
      log.info("started server on port {}", port);
      server.awaitTermination();
      scheduler.shutdown();
      return 0;
    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
      return 1;
    }
  }
}
