import sbt._

import Dependencies._
import Settings._

lazy val root = (project in file("."))
  .settings(
    name := "stream-processing",
    commonSettings
  ).aggregate(shared, wordCount, userSessions, tollApplication, tollDomain)

lazy val shared = (project in file("shared"))
  .configs(IntegrationTest.extend(Test))
  .enablePlugins(JacocoItPlugin)
  .settings(
    commonSettings,
    integrationTestSettings,
    libraryDependencies ++= Seq(
      scio,
      scioGcp,
      scioTest % Test,
      scalaLogging,
      slf4j,
      slf4jJcl,
      logback,
      scalaTest % Test,
      scalaTestPlusScalaCheck % Test,
      magnolifyScalaCheck % Test,
      diffx % Test
    )
  )

lazy val wordCount = (project in file("word-count"))
  .settings(commonSettings)
  .dependsOn(shared % "compile->compile;test->test")

lazy val userSessions = (project in file("user-sessions"))
  .settings(commonSettings)
  .dependsOn(shared % "compile->compile;test->test")

lazy val tollApplication = (project in file("toll-application"))
  .settings(commonSettings)
  .dependsOn(
    shared % "compile->compile;test->test",
    tollDomain % "compile->compile;test->test"
  )

lazy val tollDomain = (project in file("toll-domain"))
  .settings(commonSettings)
  .dependsOn(shared % "compile->compile;test->test")
