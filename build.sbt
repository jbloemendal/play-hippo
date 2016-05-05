name := """play-hippo-integration"""

version := "1.0-SNAPSHOT"

lazy val module = (project in file("module")).enablePlugins(PlayJava)

lazy val root = (project in file(".")).enablePlugins(PlayJava).aggregate(module).dependsOn(module)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs
)
