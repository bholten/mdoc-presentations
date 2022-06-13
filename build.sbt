addCommandAlias(
  "validate",
  List(
    "scalafmtCheckAll",
    "akka-streams-advanced/mdoc",
    "akka-streams-basics/mdoc"
  ).mkString(";")
)

lazy val baseSettings: Seq[Setting[_]] = Seq(
  scalaVersion := "2.13.8",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:existentials",
    "-unchecked",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"
  ),
  resolvers += Resolver.sonatypeRepo("releases")
)

lazy val mdocModule: Seq[Setting[_]] = Seq(
  mdocIn := baseDirectory.value / "mdoc",
  mdocOut := baseDirectory.value / "./docs",
  watchSources ++= (mdocIn.value ** "*.html").get
)

lazy val `akka-streams-advanced` = project
  .in(file("akka-streams-advanced"))
  .settings(moduleName := "akka-streams-advanced")
  .settings(baseSettings: _*)
  .settings(mdocModule: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % "2.6.19"
    )
  )
  .enablePlugins(MdocPlugin)

lazy val `akka-streams-basics` = project
  .in(file("akka-streams-basics"))
  .settings(moduleName := "akka-streams-basics")
  .settings(baseSettings: _*)
  .settings(mdocModule: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % "2.6.19"
    )
  )
  .enablePlugins(MdocPlugin)

