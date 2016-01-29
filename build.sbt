name := """WikiMap"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test,
  "org.apache.commons" % "commons-compress" % "1.10",
  "org.json4s" %% "json4s-native" % "3.3.0",
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
  "org.json4s" %% "json4s-jackson" % "3.3.0",
  "org.eclipse.mylyn.wikitext" % "wikitext" % "0.9.4.I20090220-1600-e3x",
  "org.eclipse.mylyn.wikitext" % "wikitext.mediawiki" % "0.9.4.I20090220-1600-e3x",
  "mysql" % "mysql-connector-java" % "5.1.38"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
