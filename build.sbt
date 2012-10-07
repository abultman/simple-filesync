name := "BruteForceFileCopier"

version := "0.1"

scalaVersion := "2.9.1"

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

libraryDependencies ++= Seq(
    "commons-lang" % "commons-lang" % "2.6",
    "commons-codec" % "commons-codec" % "1.3",
    "org.streum" %% "configrity-core" % "0.10.2"
    )

mainClass in oneJar := Some("filesync.BruteForceFileCopier")
