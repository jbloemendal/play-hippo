name := """play-hippo-integration"""
organization:= "org.onehippo"
version := "1.0"

lazy val module = (project in file("module")).enablePlugins(PlayJava)

lazy val root = (project in file(".")).enablePlugins(PlayJava).aggregate(module).dependsOn(module)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs
)
