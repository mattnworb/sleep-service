package com.mattnworb.sleep.v1.cli;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingClientInterceptor implements io.grpc.ClientInterceptor {
private static final Logger log = LoggerFactory.getLogger(LoggingClientInterceptor.class);

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      final MethodDescriptor<ReqT, RespT> methodDescriptor,
      final CallOptions callOptions,
      final Channel next) {

    String fullMethodName = methodDescriptor.getFullMethodName();

    ClientCall<ReqT, RespT> delegate = next.newCall(methodDescriptor, callOptions);

    return new ForwardingClientCall.SimpleForwardingClientCall<>(delegate) {
      @Override
      public void start(final Listener<RespT> delegate, final Metadata headers) {
        log.info("starting call to {}", fullMethodName);

        ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>
            listener =
            new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(delegate) {
              @Override
              public void onMessage(final RespT message) {
                log.info("received response to {}", fullMethodName);
                super.onMessage(message);
              }

              @Override
              public void onClose(final Status status, final Metadata trailers) {
                log.info("closing call to {}", fullMethodName);
                super.onClose(status, trailers);
              }
            };
        super.start(listener, headers);
      }

      @Override
      public void sendMessage(final ReqT message) {
        log.info("sending message to {}", fullMethodName);
        super.sendMessage(message);
      }

      @Override
      public void cancel(@Nullable final String message, @Nullable final Throwable cause) {
        log.info("call cancelled: {}", fullMethodName);
        super.cancel(message, cause);
      }
    };
  }
}
