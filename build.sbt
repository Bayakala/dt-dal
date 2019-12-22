name := "dt-dal"

version := "0.2"

scalaVersion := "2.12.8"

scalacOptions += "-Ypartial-unification"

val akkaVersion = "2.5.23"
val akkaHttpVersion = "10.1.8"

libraryDependencies := Seq(
  // for scalikejdbc
  "org.scalikejdbc" %% "scalikejdbc"       % "3.2.1",
  "org.scalikejdbc" %% "scalikejdbc-test"   % "3.2.1"   % "test",
  "org.scalikejdbc" %% "scalikejdbc-config"  % "3.2.1",
  "org.scalikejdbc" %% "scalikejdbc-streams" % "3.2.1",
  "org.scalikejdbc" %% "scalikejdbc-joda-time" % "3.2.1",
  "com.h2database"  %  "h2" % "1.4.199",
  "com.zaxxer" % "HikariCP" % "2.7.4",
  "com.jolbox" % "bonecp" % "0.8.0.RELEASE",
  "com.typesafe.slick" %% "slick" % "3.3.2",
  //for cassandra 3.6.0
  "com.datastax.cassandra" % "cassandra-driver-core" % "3.6.0",
  "com.datastax.cassandra" % "cassandra-driver-extras" % "3.6.0",
  "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "1.1.0",
  //for mongodb 4.0
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.6.0",
  "com.lightbend.akka" %% "akka-stream-alpakka-mongodb" % "1.1.0",
  "ch.qos.logback"  %  "logback-classic"   % "1.2.3",
  "io.monix" %% "monix" % "3.0.0-RC3",
  "org.typelevel" %% "cats-core" % "2.0.0-M4"
)

