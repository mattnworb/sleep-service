package com.mattnworb.sleep.v1.server;

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.services.HealthStatusManager;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  private static final Logger log = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    setupLogging(Level.INFO);

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    int port = 5000;

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
    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void setupLogging(final Level level) {
    final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    lc.reset();

    final ch.qos.logback.classic.Logger rootLogger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(level);

    final BasicConfigurator configurator = new BasicConfigurator();
    configurator.setContext(lc);
    configurator.configure(lc);
  }
}
