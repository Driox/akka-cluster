name := """test-cluster"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

resolvers += "bintray-clevercloud" at "https://dl.bintray.com/clevercloud/maven/"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "com.clevercloud" % "api-java-client" % "1.0"
) ++ deps_akka

val akka = "2.4.11"
lazy val deps_akka = Seq(
  "com.typesafe.akka"      %% "akka-actor"                 % akka              withSources(),
  "com.typesafe.akka"      %% "akka-persistence"           % akka              withSources(),
  "com.typesafe.akka"      %% "akka-cluster"               % akka              withSources(),
  "com.typesafe.akka"      %% "akka-cluster-tools"         % akka              withSources(),
  "com.typesafe.akka"      %% "akka-cluster-sharding"      % akka              withSources(),
  "com.typesafe.akka"      %% "akka-remote"                % akka              withSources(),
  "com.typesafe.akka"      %% "akka-contrib"               % akka              withSources(),
  "com.typesafe.akka"      %% "akka-slf4j"                 % akka              withSources(),
  "com.typesafe.akka"      %% "akka-multi-node-testkit"    % akka              withSources(),
  "com.typesafe.akka"      %% "akka-testkit"               % akka    % "test"  withSources(),
  "com.typesafe.akka"      %% "akka-persistence-query-experimental"  % akka    withSources(),
  "com.okumin"             %% "akka-persistence-sql-async" % "0.3.1"           withSources()
)

routesGenerator := InjectedRoutesGenerator


// ~~~~~~~~~~~~~~~~~
//Scalariform config

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(scalariform.formatter.preferences.AlignSingleLineCaseStatements, true)
  .setPreference(scalariform.formatter.preferences.AlignParameters, true)
  .setPreference(scalariform.formatter.preferences.DoubleIndentClassDeclaration, true)
  .setPreference(scalariform.formatter.preferences.PreserveDanglingCloseParenthesis, true)
