name         := "Quacker"
version      := "1.1.0"
organization := "stackableRegiments"

val scalaVersionString = "2.11.12"

scalaVersion := scalaVersionString

resolvers ++= Seq(
  "snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "releases"  at "https://oss.sonatype.org/content/repositories/releases",
  "oosnmp"    at "https://oosnmp.net/dist/release"
)

Test / unmanagedResourceDirectories += baseDirectory.value / "src/main/webapp"

scalacOptions ++= Seq("-deprecation", "-unchecked")

enablePlugins(JettyPlugin)

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.+"

libraryDependencies ++= {
  val liftVersion    = "3.5.0"
  val shiroVersion   = "1.13.0"
  val servletVersion = "2.5"
  val jettyVersion   = "9.4.54.v20240208"
  Seq(
    "commons-net"                   % "commons-net"          % "2.0",
    "org.snmp4j"                    % "snmp4j"               % "2.5.11",
    "org.mongodb"                   % "mongo-java-driver"    % "2.6.3",
    "net.sourceforge.htmlcleaner"   % "htmlcleaner"          % "2.9",
    "mysql"                         % "mysql-connector-java" % "5.1.6",
    "org.tmatesoft.svnkit"          % "svnkit"               % "1.3.4",
    "commons-io"                    % "commons-io"           % "1.4",
    "commons-codec"                 % "commons-codec"        % "1.9",
    "org.apache.httpcomponents"     % "httpcore"             % "4.1.2",
    "net.spy"                       % "spymemcached"         % "2.12.1",
    "com.h2database"                % "h2"                   % "1.4.187",
    "jivesoftware"                  % "smack"                % "3.1.0",
    "jivesoftware"                  % "smackx"               % "3.1.0",
    "jcifs"                         % "jcifs"                % "1.3.17",
    "io.github.stackableregiments" %% "common-utils"         % "1.2.+",
    "io.github.stackableregiments" %% "lift-authentication"  % "0.2.+",
    "io.github.stackableregiments" %% "cas-authentication"   % "0.2.+",
    "net.liftweb"                  %% "lift-webkit"          % liftVersion  % "compile",
    "net.liftweb"                  %% "lift-mapper"          % liftVersion  % "compile",
    "net.liftweb"                  %% "lift-mongodb"         % liftVersion,
    "net.liftweb"                  %% "lift-mongodb-record"  % liftVersion,
    "org.mongodb"                  %% "casbah"               % "2.8.2",
    "org.eclipse.jetty"             % "jetty-webapp"         % jettyVersion % "container,test",
    "org.eclipse.jetty"             % "jetty-plus"           % jettyVersion % "container,test",
    "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "container,test" artifacts Artifact(
      "javax.servlet",
      "jar",
      "jar"
    ),
    "org.specs2"                   %% "specs2"        % "2.3.12" % "test",
    "org.apache.shiro"              % "shiro-core"    % shiroVersion,
    "org.apache.shiro"              % "shiro-cas"     % shiroVersion,
    "org.apache.shiro"              % "shiro-web"     % shiroVersion,
    "javax.servlet"                 % "servlet-api"   % servletVersion,
    "io.github.stackableregiments" %% "ldap"          % "0.2.+",
    "net.databinder.dispatch"      %% "dispatch-core" % "0.11.+",
    "com.github.tototoshi"         %% "scala-csv"     % "1.2.1",
    "org.pac4j"                     % "pac4j-oauth"   % "1.7.0"
  )
}.map(
  _.excludeAll(ExclusionRule(organization = "org.slf4j"))
    .exclude("com.sun.jdmk", "jmxtools")
    .exclude("javax.jms", "jms")
    .exclude("com.sun.jmx", "jmxri")
)

javacOptions ++= Seq("-source", "1.5", "-target", "1.5")

// append -deprecation to the options passed to the Scala compiler
scalacOptions += "-deprecation"

// define the repository to publish to
publishTo := Some(
  "sonatype" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
)

// set Ivy logging to be at the highest level
ivyLoggingLevel := UpdateLogging.Full

// disable updating dynamic revisions (including -SNAPSHOT versions)
offline := false

// set the prompt (for this build) to include the project id.
ThisBuild / shellPrompt := { state =>
  Project.extract(state).currentRef.project + "> "
}

Test / testOptions += Tests.Argument("-eI")

// only show warnings and errors on the screen for all tasks (the default is Info)
//  individual tasks can then be more verbose using the previous setting
logLevel := Level.Warn

// only show 10 lines of stack traces
traceLevel := 10

credentials += Credentials(Path.userHome / ".ivy2" / "ivy-credentials")
