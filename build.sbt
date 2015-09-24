name := "spider"

version := "1.0.0"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "org.apache.httpcomponents" % "httpmime" % "4.3.5",
  "org.apache.httpcomponents" % "httpclient" % "4.3.5",
  "net.debasishg" %% "redisclient" % "3.0",
  "postgresql" % "postgresql" % "9.1-901.jdbc4",
  "mysql" % "mysql-connector-java" % "5.1.31",
  "commons-dbutils" % "commons-dbutils" % "1.5",
  "org.javassist" % "javassist" % "3.17.1-GA",
  "commons-dbcp" % "commons-dbcp" % "1.4",
  "org.scalatest" %% "scalatest" % "2.2.4",
  "com.github.detro.ghostdriver" % "phantomjsdriver" % "1.1.0",
  "com.aliyun.oss" % "aliyun-sdk-oss" % "2.0.4",
  "com.aliyun.openservices" % "aliyun-openservices" % "OTS-2.0.4",
  "org.jsoup" % "jsoup" % "1.8.3",
  "com.alibaba" % "druid" % "1.0.15",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.5.2",
  filters,
  cache
)

val root = (project in file(".")).enablePlugins(PlayScala)

TwirlKeys.templateImports += "DB._"

