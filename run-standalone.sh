#run sbt assembly to produce the jar
JAR_PATH=target/scala-2.11/my-akka-http-microservice-assembly-1.0.jar
java -cp ${JAR_PATH} AkkaHttpMicroservice
