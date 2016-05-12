name := """playhippo"""
organization:= "org.onehippo"
version := "1.0"

lazy val module = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.7"

resolvers += (
  "Hippo Maven Repository" at "http://maven.onehippo.com/maven2/"
)

libraryDependencies ++= Seq(
  "javax.jcr" % "jcr" % "2.0",
  "org.onehippo.cms7" % "hippo-repository-api" % "3.2.0",
  "org.onehippo.cms7" % "hippo-repository-connector" % "3.2.0",
  "org.onehippo.cms7" % "hippo-cms7-commons" % "2.2.0",
  "org.onehippo.cms7.hst" % "hst-api" % "3.2.0",
  "org.onehippo.cms7.hst" % "hst-commons" % "3.2.0",
  "org.onehippo.cms7.hst" % "hst-content-beans" % "3.2.0",
  javaJdbc,
  cache,
  javaWs
)
