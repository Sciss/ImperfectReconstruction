import com.typesafe.sbt.packager.linux.LinuxPackageMapping

lazy val baseName         = "Imperfect-RaspiKeys"
lazy val baseNameL        = baseName.toLowerCase
lazy val projectVersion   = "0.3.0-SNAPSHOT"

lazy val commonSettings = Seq(
  version             := projectVersion,
  organization        := "de.sciss",
  description         := "An algorithmic art project",
  homepage            := Some(url("https://github.com/Sciss/ImperfectReconstruction")),
  scalaVersion        := "2.12.5",
  licenses            := Seq(gpl2),
  scalacOptions      ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture", "-Xlint:-stars-align,_"),
  resolvers           += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/maven-releases/",
  libraryDependencies ++= Seq(
    "de.sciss"               %% "scalaosc"     % "1.1.6",
    "com.github.scopt"       %% "scopt"        % "3.7.0",
    "com.pi4j"               %  "pi4j-core"    % "1.1"
  ),
  target in assembly := baseDirectory.value,
  // ---- build info ----
  buildInfoPackage := "de.sciss.imperfect.raspikeys",
  buildInfoKeys := Seq(name, organization, version, scalaVersion, description,
    BuildInfoKey.map(homepage) { case (k, opt)           => k -> opt.get },
    BuildInfoKey.map(licenses) { case (_, Seq((lic, _))) => "license" -> lic }
  ),
  buildInfoOptions += BuildInfoOption.BuildTime
)

lazy val gpl2 = "GPL v2+" -> url("http://www.gnu.org/licenses/gpl-2.0.txt")

lazy val root = project.withId(baseNameL).in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings)

// -------------

lazy val mainClazz = "de.sciss.imperfect.raspikeys.Main"
 
mainClass in assembly := Some(mainClazz)

assemblyJarName in assembly := s"$baseName.jar"

// ---- debian package ----

enablePlugins(JavaAppPackaging, DebianPlugin)

useNativeZip

executableScriptName /* in Universal */ := baseNameL
// NOTE: doesn't work on Windows, where we have to
// provide manual file `SCALACOLLIDER_config.txt` instead!
// javaOptions in Universal ++= Seq(
//   // -J params will be added as jvm parameters
//   "-J-Xmx1024m"
//   // others will be added as app parameters
//   // "-Dproperty=true",
// )
// Since our class path is very very long,
// we use instead the wild-card, supported
// by Java 6+. In the packaged script this
// results in something like `java -cp "../lib/*" ...`.
// NOTE: `in Universal` does not work. It therefore
// also affects debian package building :-/
// We need this settings for Windows.
scriptClasspath /* in Universal */ := Seq("*")

name        in Debian := baseName
packageName in Debian := baseNameL
name        in Linux  := baseName
packageName in Linux  := baseNameL
mainClass   in Debian := Some(mainClazz)
maintainer  in Debian := s"Hanns Holger Rutz <contact@sciss.de>"
debianPackageDependencies in Debian += "java7-runtime"
packageSummary in Debian := description.value
packageDescription in Debian :=
  """Software for a video installation - Imperfect Reconstruction.
    |""".stripMargin
// include all files in src/debian in the installed base directory
linuxPackageMappings in Debian ++= {
  val n     = (name            in Debian).value.toLowerCase
  val dir   = (sourceDirectory in Debian).value / "debian"
  val f1    = (dir * "*").filter(_.isFile).get  // direct child files inside `debian` folder
  val f2    = ((dir / "doc") * "*").get
  //
  def readOnly(in: LinuxPackageMapping) =
  in.withUser ("root")
    .withGroup("root")
    .withPerms("0644")  // http://help.unc.edu/help/how-to-use-unix-and-linux-file-permissions/
  //
  val aux   = f1.map { fIn => packageMapping(fIn -> s"/usr/share/$n/${fIn.name}") }
  val doc   = f2.map { fIn => packageMapping(fIn -> s"/usr/share/doc/$n/${fIn.name}") }
  (aux ++ doc).map(readOnly)
}
