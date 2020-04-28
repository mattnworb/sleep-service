# sleep-service

A toy service to understand gRPC deadlines and cancellation better.

To build the server and CLI, run `mvn package`.

To run the server, run `java -jar server/target/sleep-server.jar`.

To run the CLI, run `java -jar cli/target/sleep-service-cli.jar localhost:5000 TIME_TO_SLEEP`.