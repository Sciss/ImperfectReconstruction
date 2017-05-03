lazy val baseName         = "Imperfect-Difference"
lazy val baseNameL        = baseName.toLowerCase
lazy val projectVersion   = "0.2.0"

lazy val commonSettings = Seq(
  version             := projectVersion,
  organization        := "de.sciss",
  description         := "An algorithmic art project",
  homepage            := Some(url(s"https://github.com/Sciss/$baseName")),
  scalaVersion        := "2.12.2",
  licenses            := Seq(gpl2),
  scalacOptions      ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture", "-Xlint"),
  resolvers           += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/maven-releases/",
  libraryDependencies ++= Seq(
    "de.sciss"          %% "fileutil"           % "1.1.2",
    "de.sciss"          %% "numbers"            % "0.1.3",
    "de.sciss"          %% "processor"          % "0.4.1",
//    "com.mortennobel"   %  "java-image-scaling" % "0.8.6",  // includes jh filters
    "de.sciss"          %% "audiowidgets-swing" % "1.10.2",
    "de.sciss"          %% "desktop"            % "0.7.3",
    "de.sciss"          %% "guiflitz"           % "0.5.1",
    "de.sciss"          %% "play-json-sealed"   % "0.4.1",
    "de.sciss"          %% "kollflitz"          % "0.2.1",
    // "de.sciss"          %  "submin"             % "0.2.1",
    "com.github.scopt"  %% "scopt"              % "3.5.0",
    "de.sciss"          %% "scissdsp"           % "1.2.3",
    "de.sciss"          %% "scalaaudiofile"     % "1.4.6",
    "de.sciss"          %% "fscape"             % "2.6.4",
    "com.pi4j"          %  "pi4j-core"          % "1.1",
    "de.sciss"          %  "jrpicam"            % "0.2.0"
//    "de.sciss"          %% "fscapejobs"         % "1.5.0"
  ),
  target in assembly := baseDirectory.value
)

lazy val gpl2 = "GPL v2+" -> url("http://www.gnu.org/licenses/gpl-2.0.txt")

lazy val root = Project(id = baseNameL, base = file("."))
  .settings(commonSettings)

// -------------

mainClass in assembly := Some("de.sciss.imperfect.difference.Exposure")

assemblyJarName in assembly := s"$baseName.jar"
