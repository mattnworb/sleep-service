package com.mattnworb.sleep.v1.server;

import com.google.protobuf.TextFormat;
import com.mattnworb.sleep.v1.SleepRequest;
import com.mattnworb.sleep.v1.SleepResponse;
import com.mattnworb.sleep.v1.SleepServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SleepServiceImpl extends SleepServiceGrpc.SleepServiceImplBase {
  private static final Logger log = LoggerFactory.getLogger(SleepServiceImpl.class);

  private final ScheduledExecutorService scheduledExecutorService;

  public SleepServiceImpl(final ScheduledExecutorService scheduledExecutorService) {
    this.scheduledExecutorService = scheduledExecutorService;
  }

  @Override
  public void sleep(
      final SleepRequest request, final StreamObserver<SleepResponse> responseObserver) {

    final int sleepTimeMillis = request.getSleepTimeMillis();
    if (sleepTimeMillis == 0) {
      StatusException ex =
          Status.INVALID_ARGUMENT.withDescription("sleepTimeMillis must be > 0").asException();
      responseObserver.onError(ex);
    }

    scheduledExecutorService.schedule(
        () -> {
          final SleepResponse response =
              SleepResponse.newBuilder().setTimeSleptMillis(sleepTimeMillis).build();

          log.info("sending response: {}", TextFormat.shortDebugString(response));
          responseObserver.onNext(response);
          responseObserver.onCompleted();
        },
        sleepTimeMillis,
        TimeUnit.MILLISECONDS);
  }
}
