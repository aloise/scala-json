ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.8.2"

lazy val root = (project in file("."))
  .settings(
    name := "scala-json",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-json" % "0.7.3",
      "org.typelevel" %% "shapeless3-deriving" % "3.4.3",
      "dev.zio" %% "zio-test" % "2.1.9" % Test,
      "dev.zio" %% "zio-test-sbt" % "2.1.9" % Test
    ),
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Wunused:all"),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
