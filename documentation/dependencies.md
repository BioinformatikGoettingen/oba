# Dependencies of OBA

## Java

Java version 7 or 8 is needed. Jersey is compiled with
Java 7, so that Java 6 can be used only after a downgrade
of Jersey to 1.x. However, this will require changes in
in RestServer.java and StorageHandler.java. Also the
property names changed from jersey 1.x to 2.x


Java 9 is not tested yet.


