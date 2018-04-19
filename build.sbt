import sbtcrossproject.{crossProject, CrossType}

inThisBuild(List(
  organization := "org.tmt",
  scalaVersion := "2.12.4",
  version := "0.1.0-SNAPSHOT",
  resolvers += "jitpack" at "https://jitpack.io",
  scalacOptions ++= Seq(
    "-encoding",
    "UTF-8",
    "-feature",
    "-unchecked",
    "-deprecation",
    //"-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Xfuture",
    //      "-Xprint:typer"
  )
))

lazy val `esw-sequencer` = project
  .in(file("."))
  .aggregate(
    `sequencer-api-js`,
    `sequencer-api-jvm`,
    `sequencer-js-app`,
    `sequencer-js-client`,
    `sequencer-macros`,
    `sequencer-framework`,
    `csw-messages-js`,
    `csw-messages-jvm`,
  )

lazy val `sequencer-api` = crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure)
lazy val `sequencer-api-js` = `sequencer-api`.js
lazy val `sequencer-api-jvm` = `sequencer-api`.jvm

lazy val `sequencer-js-client` = project
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(`sequencer-api-js`)
  .settings(
    webpackBundlingMode := BundlingMode.LibraryOnly(),
    useYarn := true,
    scalacOptions += "-P:scalajs:sjsDefinedByDefault",
    libraryDependencies ++= Seq(
      SharedLibs.`boopickle`.value,
      Covenant.`covenant-http`.value,
      Covenant.`covenant-ws`.value,
      SharedLibs.scalaTest.value % Test,
    )
  )

lazy val `sequencer-js-app` = project
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(`sequencer-js-client`)
  .settings(
    useYarn := true,
    scalaJSUseMainModuleInitializer := true,
    scalacOptions += "-P:scalajs:sjsDefinedByDefault",
    libraryDependencies ++= Seq(
      SharedLibs.scalaTest.value % Test
    )
  )

lazy val `sequencer-macros` = project
  .settings(
    libraryDependencies ++= Seq(
      Libs.`scala-async`,
      Libs.`scala-reflect`,
    )
  )

lazy val `sequencer-framework` = project
  .enablePlugins(JavaAppPackaging)
  .dependsOn(`sequencer-macros`, `sequencer-api-jvm`)
  .settings(
    name := "sequencer-framework",
    libraryDependencies ++= Seq(
      Libs.`scala-reflect`,
      Libs.`akka-http-cors`,
      Libs.`scala-compiler`,
      Akka.`akka-stream`,
      Akka.`akka-typed`,
      Akka.`akka-typed-testkit`,
      Ammonite.`ammonite`,
      Ammonite.`ammonite-sshd`,
      Libs.`jgit`,
      Libs.`enumeratum`,
      Libs.`scala-async`,
      SharedLibs.`boopickle`.value,
      Covenant.`covenant-http`.value,
      Covenant.`covenant-ws`.value,
      SharedLibs.scalaTest.value % Test,
    ),
  )

lazy val `csw-messages` = crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure)
  .settings(
    libraryDependencies ++= Seq(
      Enumeratum.`enumeratum`.value,
      Libs.`play-json`.value,
      Libs.`play-json-extensions`.value,
      Libs.`scalapb-runtime`.value,
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      AkkaJs.`akka-typed`.value % Provided,
      AkkaJs.`akka-stream`.value % Provided,
      Libs.`scalajs-java-time`.value
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      Akka.`akka-typed`,
      Akka.`akka-stream`,
      Libs.`scala-java8-compat`,
      Chill.`chill-bijection`
    )
  )
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ),
    PB.protoSources in Compile := Seq(file("csw-messages/src/main/protobuf")),
  )

lazy val `csw-messages-js` = `csw-messages`.js
lazy val `csw-messages-jvm` = `csw-messages`.jvm
