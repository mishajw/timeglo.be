name := "BackEnd"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-compress" % "1.10",
  "org.json4s" %% "json4s-native" % "3.3.0",
  "com.github.mauricio" %% "postgresql-async" % "0.2.18"
)
