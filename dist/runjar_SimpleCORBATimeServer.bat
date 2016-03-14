REM Use this bat file when the jacorb libraries are not part of the jar file.

java -Xbootclasspath/p:lib\jacorb.jar;lib/logkit-1.2.jar;lib/wrapper-3.1.0.jar;lib/slf4j-jdk14-1.5.6.jar;lib/slf4j-api-1.5.6.jar -jar SimpleCORBATimeServer.jar -ORBInitRef.NameService=corbaloc::localhost:2089/StandardNS/NameServer-POA/_root