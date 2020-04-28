package com.mattnworb.sleep.v1.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class Main {
  public static void main(String[] args) {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    Cli cli = new Cli(System.out, System.err);

    int status = cli.run(args);
    if (status != 0) {
      System.exit(status);
    }
  }
}
