lazy val baseName         = "Imperfect-Difference"
lazy val baseNameL        = baseName.toLowerCase
lazy val projectVersion   = "0.3.0-SNAPSHOT"

lazy val commonSettings = Seq(
  version             := projectVersion,
  organization        := "de.sciss",
  description         := "An algorithmic art project",
  homepage            := Some(url(s"https://github.com/Sciss/$baseName")),
  scalaVersion        := "2.12.5",
  licenses            := Seq(gpl2),
  scalacOptions      ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture", "-Xlint"),
  resolvers           += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/maven-releases/",
  libraryDependencies ++= Seq(
    "de.sciss"          %% "fileutil"           % "1.1.3",
    "de.sciss"          %% "numbers"            % "0.1.5",
    "de.sciss"          %% "processor"          % "0.4.1",
    "de.sciss"          %% "audiowidgets-swing" % "1.12.0",
    "de.sciss"          %% "desktop"            % "0.9.2",
    "de.sciss"          %% "guiflitz"           % "0.6.0",
    "de.sciss"          %% "play-json-sealed"   % "0.4.1",
    "de.sciss"          %% "kollflitz"          % "0.2.2",
    "com.github.scopt"  %% "scopt"              % "3.7.0",
    "de.sciss"          %% "scissdsp"           % "1.3.0",
    "de.sciss"          %% "audiofile"          % "1.5.0",
    "de.sciss"          %% "fscape"             % "2.13.0",
    "com.pi4j"          %  "pi4j-core"          % "1.1",
    "de.sciss"          %  "jrpicam"            % "0.2.0"
  ),
  target in assembly := baseDirectory.value,
  assemblyMergeStrategy in assembly := {
    case PathList("de", "sciss", "lucre", "stm", _ @ _*) => MergeStrategy.first
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)

lazy val gpl2 = "GPL v2+" -> url("http://www.gnu.org/licenses/gpl-2.0.txt")

lazy val root = Project(id = baseNameL, base = file("."))
  .settings(commonSettings)

// -------------

mainClass in assembly := Some("de.sciss.imperfect.difference.Exposure")

assemblyJarName in assembly := s"$baseName.jar"
