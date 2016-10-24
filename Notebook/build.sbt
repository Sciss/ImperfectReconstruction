lazy val baseName         = "Imperfect-Notebook"
lazy val baseNameL        = baseName.toLowerCase
lazy val projectVersion   = "0.1.0-SNAPSHOT"

lazy val commonSettings = Seq(
  version             := projectVersion,
  organization        := "de.sciss",
  description         := "An algorithmic art project",
  homepage            := Some(url(s"https://github.com/Sciss/$baseName")),
  scalaVersion        := "2.11.8",
  licenses            := Seq(gpl2),
  scalacOptions      ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture", "-Xlint"),
  resolvers           += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/maven-releases/",
  libraryDependencies ++= Seq(
    "de.sciss"               %% "fileutil"        % "1.1.2",
    "de.sciss"               %% "numbers"         % "0.1.3",
    "de.sciss"               %% "kollflitz"       % "0.2.0",
    "com.github.scopt"       %% "scopt"           % "3.5.0",
    "de.sciss"               %% "fscape"          % "2.3.0",
    "org.apache.xmlgraphics" %  "batik-parser"    % "1.8",
    "org.swinglabs.swingx"   % "swingx-graphics"  % "1.6.5"
  )
  // target in assembly := baseDirectory.value
)

lazy val gpl2 = "GPL v2+" -> url("http://www.gnu.org/licenses/gpl-2.0.txt")

lazy val root = Project(id = baseNameL, base = file("."))
  .settings(commonSettings)

// -------------
// 
// mainClass in assembly := Some("de.sciss.imperfect.orthogonal.Orthogonal")
// 
// assemblyJarName in assembly := s"$baseName.jar"
