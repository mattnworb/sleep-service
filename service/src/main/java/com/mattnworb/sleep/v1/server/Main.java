package com.mattnworb.sleep.v1.server;

import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  private static final Logger log = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    int port = 5000;
    Server server =
        NettyServerBuilder.forPort(port)
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
}
