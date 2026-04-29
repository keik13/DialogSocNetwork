ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.7"
ThisBuild / scalafmtOnCompile := true

lazy val root = (project in file("."))
  .settings(
    name := "DialogSocNetwork",
    assemblyJarName := s"${name.value}-${version.value}.jar",
    assembly / assemblyOutputPath := file(s"target/${assemblyJarName.value}"),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "services", "java.sql.Driver", _ @_*) =>
        MergeStrategy.first
      case PathList(
            "META-INF",
            "services",
            "org.flywaydb.core.extensibility.Plugin",
            _ @_*
          ) =>
        MergeStrategy.first
      case PathList("META-INF", _ @_*) => MergeStrategy.discard
      case _                           => MergeStrategy.first
    },
    Test / fork := true,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.1.25",
      "dev.zio" %% "zio-http" % "3.8.1",
      "dev.zio" %% "zio-json" % "0.7.44",
      "dev.zio" %% "zio-config-typesafe" % "4.0.7",
      "dev.zio" %% "zio-config-magnolia" % "4.0.7",
      "dev.zio" %% "zio-logging" % "2.5.3",
      "io.getquill" %% "quill-jdbc-zio" % "4.8.6",
      "com.github.jwt-scala" %% "jwt-zio-json" % "11.0.4",
      "org.postgresql" % "postgresql" % "42.7.10",
      "org.flywaydb" % "flyway-core" % "9.22.3",
      "dev.zio" %% "zio-test" % "2.1.25" % Test,
      "dev.zio" %% "zio-test-sbt" % "2.1.25" % Test,
      "org.testcontainers" % "postgresql" % "1.21.4" % Test,
    ),

    scalacOptions ++= Seq(
      "-language:Scala3",
      "-source:3.0",
      "-deprecation",
      "-encoding",
      "utf-8",
      "-explain"
    )
  )
