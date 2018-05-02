scalaVersion := "2.9.3"

scalaSource in Compile <<= baseDirectory(_ / "src")

scalacOptions ++= Seq("-deprecation", "-unchecked",// "-feature",
  "-Xlint", "-Xfatal-warnings",
    "-encoding", "us-ascii")

//libraryDependencies += "org.nlogo" % "NetLogoHeadless" % "5.x-9fdce9bf" from  "http://ccl.northwestern.edu/devel/NetLogoHeadless-9fdce9bf.jar"
libraryDependencies += "org.nlogo" % "NetLogo" % "5.1.0" from "http://ccl.northwestern.edu/netlogo/5.1/NetLogo.jar"

libraryDependencies += "org.apache.commons" % "commons-math3" % "3.3"


artifactName := { (_, _, _) => "morphology.jar" }

packageOptions := Seq(
  Package.ManifestAttributes(
    ("Extension-Name", "morphology"),
    ("Class-Manager", "Morphology"),
    ("NetLogo-Extension-API-Version", "5.0")))

packageBin in Compile <<= (packageBin in Compile, baseDirectory, streams) map {
  (jar, base, s) =>
    IO.copyFile(jar, base / "morphology.jar")
    Process("pack200 --modification-time=latest --effort=9 --strip-debug " +
            "--no-keep-file-order --unknown-attribute=strip " +
            "morphology.jar.pack.gz morphology.jar").!!
    if(Process("git diff --quiet --exit-code HEAD").! == 0) {
      Process("git archive -o morphology.zip --prefix=morphology/ HEAD").!!
      IO.createDirectory(base / "morphology")
      IO.copyFile(base / "morphology.jar", base / "morphology" / "morphology.jar")
      IO.copyFile(base / "morphology.jar.pack.gz", base / "morphology" / "morphology.jar.pack.gz")
      Process("zip morphology.zip morphology/morphology.jar morphology/morphology.jar.pack.gz").!!
      IO.delete(base / "sample-scala")
    }
    else {
      s.log.warn("working tree not clean; no zip archive made")
      IO.delete(base / "morphology.zip")
    }
    jar
  }

cleanFiles <++= baseDirectory { base =>
  Seq(base / "morphology.jar",
      base / "morphology.jar.pack.gz",
      base / "morphology.zip") }
