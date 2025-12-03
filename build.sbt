versionWithGit
git.baseVersion := "2.0"
git.useGitDescribe := true

name := "app.stackableregiments.quacker"
version in ThisBuild := "develop"
scalaVersion in ThisBuild := "2.12.15"

val jettyVersion               = "11.0.23"

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules_2.12" % "scala-xml" % VersionScheme.Always
ThisBuild / evictionErrorLevel                               := Level.Info

organization := "stackableRegiments"

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      sbtVersion,
      BuildInfoKey.action("buildTime") {
        System.currentTimeMillis
      },
      BuildInfoKey.action("gitBranch") {
        git.gitCurrentBranch.value
      },
      BuildInfoKey.action("gitCommit") {
        git.gitHeadCommit.value
      }
    ),
    buildInfoPackage := "code.buildInfo"
  )

ThisBuild / scalafmtOnCompile := false

reporterConfig := reporterConfig.value.withColumnNumbers(true)

reporterConfig := reporterConfig.value.withShowLegend(true)

compileOrder := CompileOrder.ScalaThenJava

val otelAgentJar = Def.task {
  (Runtime / managedClasspath).value
    .find(_.data.getName.contains("opentelemetry-javaagent"))
    .map(_.data.getAbsolutePath)
    .getOrElse("")
}

javaOptions in Jetty ++= Seq(
  "-Djavax.net.ssl.keyStore=keystore.jks",
  "-Djavax.net.ssl.keyStorePassword=changeit",
  "-Dquacker.configDirectoryLocation=monitoringDashboardConfig",
  "-Dquacker.appConfigDirectoryLocation=appConf",
  "-Drun.mode=production",
  "-Dlogback.configurationFile=appConf/logback.xml"
)

//logLevel in ThisBuild := Level.Error
logLevel in ThisBuild := Level.Info

resolvers in ThisBuild ++= Seq(
  DefaultMavenRepository,
  Resolver.sonatypeRepo("public"),
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("staging"),
  Resolver.typesafeIvyRepo("releases"),
  Resolver.typesafeIvyRepo("snapshots"),
  "maven" at "https://mvnrepository.com/artifact/",
  "IHTSDO" at "https://maven.ihtsdotools.org/content/repositories/releases/",
  "snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "releases" at "https://oss.sonatype.org/content/repositories/releases",
  "mavenCentral" at "https://mvnrepository.com/artifact"
)

enablePlugins(JettyPlugin)


val jettyMem  = sys.env.get("SBT_JETTY_MEM").getOrElse("2048")
val jettyCpus = sys.env.get("SBT_JETTY_CPUS").getOrElse("4")

Jetty / javaOptions ++= Seq(
//  Otel
//  GCloud settings 
//  "-Dotel.resource.providers.gcp.enabled=true",
//  "-Dotel.traces.exporter=google_cloud_trace",
//  "-Dotel.metrics.exporter=google_cloud_monitoring",
//  "-javaagent:.kube/extraJars/opentelemetry-javaagent-2.12.0.jar",
//  "-Dotel.javaagent.extensions=.kube/extraJars/exporter-auto-0.33.0-alpha-shaded.jar",


//  local
  "-Dotel.exporter.otlp.insecure=true",
  "-Dotel.exporter.otlp.protocol=grpc",
  "-Dotel.exporter.otlp.endpoint=http://collector.localhost:4317",
  s"-javaagent:${otelAgentJar.value}",

    // Customize this 
  "-Dotel.service.name=local.quacker.pathify.com",

// common settings
  "-Dotel.javaagent.enabled=true",
  //Otel end
  "-Xmx%sM".format(jettyMem),
  "-Xms%sM".format(jettyMem),
  "-XX:ActiveProcessorCount=%s".format(jettyCpus),
  "--add-opens",
  "java.base/java.net=ALL-UNNAMED",
  "--add-opens",
  "java.base/java.lang=ALL-UNNAMED",
  "--add-opens",
  "java.base/java.time=ALL-UNNAMED",
  "--add-opens",
  "java.base/java.lang.invoke=ALL-UNNAMED",
  "--add-opens",
  "java.base/java.util=ALL-UNNAMED",
  "-Djavax.net.ssl.keyStore=keystore.jks",
  "-Djavax.net.ssl.keyStorePassword=changeit",
  "-Dorg.eclipse.jetty.util.log.class=org.apache.logging.log4j.appserver.jetty.Log4j2Logger",
  "-Dquacker.configDirectoryLocation=monitoringDashboardConfig",
  "-Dquacker.appConfigDirectoryLocation=appConf",
  "-Drun.mode=production",
  "-Dlogback.configurationFile=appConf/logback.xml",
  "-Dslf4j.provider=ch.qos.logback.classic.spi.LogbackServiceProvider"

)

Jetty / containerLibs := Seq("org.eclipse.jetty" % "jetty-runner" % jettyVersion intransitive ())
Jetty / containerPort := 8666
Jetty / containerArgs := Seq("--config", "jetty.xml")

 val logbackVersion = "1.5.21"
 val slf4jVersion = "2.0.17" 
 
libraryDependencies in ThisBuild ++= {
  val liftVersion = "3.4.3"
  val shiroVersion = "1.2.2"
  val scalaTestVersion = "3.3.0-SNAP4"
  val otelVersion = "1.56.0"
  val otelAgentVersion = "2.22.0"

  Seq(
    "net.liftweb" %% "lift-webkit" % liftVersion,
    "net.liftweb" %% "lift-mapper" % liftVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.9",
    "org.specs2" %% "specs2-core" % "3.9.4" % "test",
    "com.h2database" % "h2" % "1.4.187",
    "com.github.dakatsuka" %% "akka-http-oauth2-client" % "0.1.0",
    "io.lemonlabs" %% "scala-uri" % "1.4.4",
    "com.google.api-client" % "google-api-client" % "1.25.0",
    "com.softwaremill.sttp" %% "core" % "1.5.0",
    "com.softwaremill.sttp" %% "okhttp-backend" % "1.5.11",
    "org.ekrich" %% "sconfig" % "0.8.0",
    "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided",
    "org.eclipse.jetty" % "jetty-webapp" % jettyVersion,
    "org.eclipse.jetty" % "jetty-server" % jettyVersion,
    "org.eclipse.jetty" % "jetty-util"   % jettyVersion,
//    "net.rcarz" % "jira-client" % "0.6.3-IHTSDO",
    "javax.mail" % "javax.mail-api" % "1.6.2",
    "com.sun.mail" % "javax.mail" % "1.6.2",
    "com.hacklanta" %% "lift-formality_3.3" % "1.2.0",
    "io.github.classgraph" % "classgraph" % "4.6.18",
    "com.sksamuel.scrimage" % "scrimage-core" % "4.0.6",
    "com.rklaehn" % "radixtree_2.12" % "0.5.1",
    "com.joestelmach" % "natty" % "0.11",
    //for resource loading
    "org.springframework" % "spring-core" % "5.1.5.RELEASE",
    //for testing
    "org.specs2"     %% "specs2-core"              % "3.9.5"          % Test,
    "org.scalatest"  %% "scalatest"                % scalaTestVersion % Test,
    "org.scalatest"  %% "scalatest-shouldmatchers" % scalaTestVersion % Test,
    "org.scalatest"  %% "scalatest-app"            % scalaTestVersion % Test,
    "org.scalacheck" %% "scalacheck"               % "1.14.3"         % Test,
    // for ssh
    "com.jcraft" % "jsch" % "0.1.55",
    // for JWT encoding/decoding
    "com.pauldijou" %% "jwt-core" % "4.1.0",
    // for serialization into and out of the db
    "com.twitter" %% "chill" % "0.9.5",
    "com.twitter" %% "bijection-core" % "0.9.7",
    "com.twitter" %% "bijection-json" % "0.9.7",
    "com.twitter" %% "bijection-util" % "0.9.7",
    // for cron triggering
    "org.quartz-scheduler" % "quartz" % "2.3.2",
    // for google pubsub
    "com.google.cloud" % "google-cloud-pubsub" % "1.98.0",
    // for google cloud storage
    "com.google.cloud" % "google-cloud-storage" % "1.101.0",
    // for google bigquery
    "com.google.cloud" % "google-cloud-bigquery" % "1.101.0",
    // for google sheets
    "com.google.apis" % "google-api-services-sheets" % "v4-rev581-1.25.0",
    // for google calendar
    "com.google.apis" % "google-api-services-calendar" % "v3-rev401-1.25.0",
    // for gmail
    //"com.google.apis" % "google-api-services-gmail" % "v1-rev110-1.25.0",
    //"com.google.apis" % "google-api-services-gmail" % "v1-rev110-1.25.0",
    "com.google.apis" % "google-api-services-gmail" % "v1-rev20200110-1.29.2",
    // for drive
    "com.google.apis" % "google-api-services-drive" % "v3-rev188-1.25.0",
    // for google analytics
    "com.google.apis" % "google-api-services-analytics" % "v3-rev169-1.25.0",
    "com.google.apis" % "google-api-services-analyticsreporting" % "v4-rev174-1.25.0",
    // for cloud sql
    "com.google.cloud.sql" % "postgres-socket-factory" % "1.0.15",
    "com.google.cloud.sql" % "mysql-socket-factory-connector-j-8" % "1.0.15",
    // for parsing ical
    "net.sf.biweekly" % "biweekly" % "0.6.3",
    // for mysql
    "mysql" % "mysql-connector-java" % "8.0.19",
    // for postgres
    "org.postgresql" % "postgresql" % "42.2.9",
    // for csv parsing
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-csv" % "2.10.0",
    // for AWS SDK
    "com.amazonaws" % "aws-java-sdk" % "1.11.688",
    // for phone number parsing
    "com.googlecode.libphonenumber" % "libphonenumber" % "8.11.2",
    // for htmlUnit webbrowser robot
    "net.sourceforge.htmlunit" % "htmlunit" % "2.37.0",
    // for file type detection
    "org.apache.tika" % "tika-core" % "1.23",
    //for apache commons dbcp db connection pooling
    "org.apache.commons" % "commons-dbcp2" % "2.7.0",
    //for SAML
    "org.opensaml" % "opensaml" % "2.6.4",
    "javax.xml" % "jaxb-api" % "2.1",
    /*telnet & munin*/
    "commons-net" % "commons-net" % "2.0",
    /*snmp*/
    "org.snmp4j" % "snmp4j" % "3.4.2",
    /*mongo*/
    "org.mongodb" % "mongo-java-driver" % "2.6.3",
    /*html-cleaner for html parsing*/
    "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.9",
    /*subversion*/
    "org.tmatesoft.svnkit" % "svnkit" % "1.3.4",
    /*general*/
    "commons-io" % "commons-io" % "1.4",
    "commons-codec" % "commons-codec" % "1.9",
    /*http*/
    //"org.apache.httpcomponents" % "httpcore" % "4.1.2",
    "org.apache.httpcomponents" % "httpcore" % "4.4.15",
		"org.apache.httpcomponents.core5" % "httpcore5" % "5.1.1",
		"org.apache.httpcomponents.client5" % "httpclient5" % "5.1.2",
    /*memcached*/
    //"spy" % "spymemcached" % "2.6",
    "net.spy" % "spymemcached" % "2.12.1",
    /*h2*/
    "com.h2database" % "h2" % "1.4.187",
    /*xmpp*/
    "jivesoftware" % "smack" % "3.1.0",
    "jivesoftware" % "smackx" % "3.1.0",
    /*samba*/
    "jcifs" % "jcifs" % "1.3.17",
    /*Security stack*/
    "org.apache.shiro" % "shiro-core" % shiroVersion,
    "org.apache.shiro" % "shiro-cas" % shiroVersion,
    "org.apache.shiro" % "shiro-web" % shiroVersion,
    /*Testing*/
    "org.specs2" % "specs2-cats_2.12" % "4.10.2" % "test",
    "org.specs2" % "specs2_2.12" % "3.8.9" % "test",
    //mongo-lift
    "com.mongodb.casbah" % "casbah_2.9.1" % "2.1.5-1",
    "net.liftweb" %% "lift-mongodb" % liftVersion,
    "net.liftweb" %% "lift-mongodb-record" % liftVersion,
    /*Http management*/
    "net.databinder.dispatch" %% "dispatch-core" % "0.13.+",
    /*Parsing*/
    "com.github.tototoshi" %% "scala-csv" % "1.3.6",
    /*OAuth authentication*/
    "org.pac4j" % "pac4j-oauth" % "1.7.0",
       /*Open telemetry*/
    "io.opentelemetry"              % "opentelemetry-api"       % otelVersion,
    "io.opentelemetry.javaagent"    % "opentelemetry-javaagent" % otelAgentVersion %  "runtime",
    /* logback through google */
    "com.google.cloud"                 % "google-cloud-logging"                          % "3.23.0",
    "com.google.cloud"                 % "google-cloud-logging-logback"                  % "0.131.11-alpha",
    "com.google.cloud"                 % "google-cloud-logging-servlet-initializer"      % "0.2.13-alpha",
    "io.opentelemetry.instrumentation" % "opentelemetry-logback-mdc-1.0"                 % "2.21.0-alpha",
  )
}.map(
  _.excludeAll(ExclusionRule(organization = "org.slf4j"))
    .exclude("com.sun.jdmk", "jmxtools")
    .exclude("javax.jms", "jms")
    .exclude("com.sun.jmx", "jmxri")
    .exclude("log4j", "log4j")
)

libraryDependencies ++= Seq(
"org.slf4j"        % "slf4j-api"                                % slf4jVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
    "io.opentelemetry.instrumentation" % "opentelemetry-logback-mdc-1.0" % "2.21.0-alpha"
  )

scalacOptions in ThisBuild ++= Seq(
  "-language:existentials",
  "-deprecation",
  "-unchecked",
  "-feature",
  "-Xfuture",
  "-Xlint:adapted-args",
  "-Xlint:doc-detached",
  "-Xlint:inaccessible",
  "-Xlint:infer-any",
  "-Xlint:missing-interpolator",
  "-Xlint:private-shadow",
  "-Xlint:poly-implicit-overload",
  "-Xlint:type-parameter-shadow",
  "-Xlint:option-implicit",
  "-Xlint:delayedinit-select",
  "-Xlint:by-name-right-associative",
  "-Xlint:package-object-classes",
  "-Xlint:unsound-match",
  "-Ywarn-dead-code",
  "-Ypartial-unification",
  "-Ywarn-value-discard"
)

// set Ivy logging to be at the highest level
ivyLoggingLevel := UpdateLogging.Full


// disable updating dynamic revisions (including -SNAPSHOT versions)
offline := false

// set the prompt (for this build) to include the project id.
shellPrompt in ThisBuild := { state =>
  Project.extract(state).currentRef.project + "> "
}

// set the prompt (for the current project) to include the username
shellPrompt := { state =>
  System.getProperty("user.name") + "> "
}

// disable printing timing information, but still print [success]
showTiming := true

// disable printing a message indicating the success or failure of running a task
showSuccess := true

// change the format used for printing task completion time
timingFormat := {
  import java.text.DateFormat
  DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
}

testOptions in Test += Tests.Argument("-eI")

// don't aggregate clean (See FullConfiguration for aggregation details)
aggregate in clean := false

// only show warnings and errors on the screen for compilations.
//  this applies to both test:compile and compile and is Info by default
logLevel in compile := Level.Warn

// only show warnings and errors on the screen for all tasks (the default is Info)
//  individual tasks can then be more verbose using the previous setting
logLevel := Level.Warn

// only store messages at info and above (the default is Debug)
//   this is the logging level for replaying logging with 'last'
//persistLogLevel := Level.Debug

// only show 10 lines of stack traces
traceLevel := 10

// only show stack traces up to the first sbt stack frame
traceLevel := 0

credentials += Credentials(Path.userHome / ".ivy2" / "ivy-credentials")
