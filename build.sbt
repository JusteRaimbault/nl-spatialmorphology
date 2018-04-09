enablePlugins(org.nlogo.build.NetLogoExtension)

netLogoExtName      := "Morphology"

netLogoClassManager := "Morphology"

scalaVersion           := "2.11.7"

scalaSource in Compile := baseDirectory.value / "src"

scalacOptions          ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-encoding", "us-ascii")

libraryDependencies += "org.apache.commons" % "commons-math3" % "3.3"

netLogoVersion := "6.0.0-M4"

fork in run := true