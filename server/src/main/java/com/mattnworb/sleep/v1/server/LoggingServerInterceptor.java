package com.mattnworb.sleep.v1.server;

import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingServerInterceptor implements io.grpc.ServerInterceptor {
  private static final Logger log = LoggerFactory.getLogger(LoggingServerInterceptor.class);

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      final ServerCall<ReqT, RespT> call,
      final Metadata headers,
      final ServerCallHandler<ReqT, RespT> next) {

    String fullMethodName = call.getMethodDescriptor().getFullMethodName();

    ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> forwardingCall =
        new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
          @Override
          public void sendMessage(final RespT message) {
            super.sendMessage(message);
            log.info("sent message for method {}", fullMethodName);
          }
        };

    ServerCall.Listener<ReqT> listener = next.startCall(forwardingCall, headers);

    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {
      @Override
      public void onMessage(final ReqT message) {
        super.onMessage(message);
        log.info("received message for method {}", fullMethodName);
      }

      @Override
      public void onCancel() {
        super.onCancel();
        log.info("call to {} cancelled", fullMethodName);
      }

      @Override
      public void onComplete() {
        super.onComplete();
        log.info("call to {} complete", fullMethodName);
      }
    };
  }
}
