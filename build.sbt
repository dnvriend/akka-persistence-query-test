name := "akka-persistence-query-test"

organization := "com.github.dnvriend"

version := "1.0.0"

scalaVersion := "2.11.8"

resolvers += Resolver.jcenterRepo

val akkaVersion = "2.4.17"

// inmemory
libraryDependencies += "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.0.0-M1-SNAPSHOT"

// jdbc
//libraryDependencies += "com.github.dnvriend" %% "akka-persistence-jdbc" % "2.6.12"
//libraryDependencies += "com.h2database" % "h2" % "1.4.193"

// leveldb
libraryDependencies += "com.typesafe.akka" %% "akka-persistence" % akkaVersion
libraryDependencies += "org.iq80.leveldb" % "leveldb" % "0.9"
libraryDependencies += "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"

// cassandra
//libraryDependencies += "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.22"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.9"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % Test

fork in Test := true

parallelExecution in Test := false

licenses +=("Apache-2.0", url("http://opensource.org/licenses/apache2.0.php"))

// enable scala code formatting //
import java.text.SimpleDateFormat
import com.typesafe.sbt.SbtScalariform
import scalariform.formatter.preferences._

// Scalariform settings
SbtScalariform.autoImport.scalariformPreferences := SbtScalariform.autoImport.scalariformPreferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)

// enable updating file headers //
import de.heikoseeberger.sbtheader.license.Apache2_0

lazy val year: String = {
  new SimpleDateFormat("yyyy").format(new java.util.Date())
}

headers := Map(
  "scala" -> Apache2_0(year, "Dennis Vriend"),
  "conf" -> Apache2_0(year, "Dennis Vriend", "#")
)

enablePlugins(AutomateHeaderPlugin)