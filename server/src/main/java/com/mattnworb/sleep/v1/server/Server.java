package com.mattnworb.sleep.v1.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.services.HealthStatusManager;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {
  private static final Logger log = LoggerFactory.getLogger(Server.class);

  private io.grpc.Server server;
  private ScheduledExecutorService scheduler;

  void start(int port) throws IOException {
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("sleep-service-scheduler-%d").build();
    this.scheduler = Executors.newScheduledThreadPool(5, threadFactory);

    HealthStatusManager healthStatusManager = new HealthStatusManager();

    this.server =
        NettyServerBuilder.forPort(port)
            .addService(healthStatusManager.getHealthService())
            .addService(ProtoReflectionService.newInstance())
            .addService(new SleepServiceImpl(scheduler))
            .intercept(new LoggingServerInterceptor())
            .build();

    server.start();
    log.info("started server on port {}", port);
  }

  void awaitTermination() throws InterruptedException {
    server.awaitTermination();
    scheduler.shutdown();
  }
}
