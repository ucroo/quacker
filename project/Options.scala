object Options {

  object Java {

    def JettyOptions(otelEnabled: Boolean, otelAgentJar: String) = {
        Seq(
          "-Djavax.net.ssl.keyStore=keystore.jks",
          "-Djavax.net.ssl.keyStorePassword=changeit",
          "-Dslf4j.provider=ch.qos.logback.classic.spi.LogbackServiceProvider",
          s"-Dotel.service.name=quacker.localhost.com",
          "-Dotel.exporter.otlp.protocol=grpc",
          "-Dotel.exporter.otlp.insecure=true",
          "-Dotel.resource.providers.gcp.enabled=true",
          "-Dotel.exporter.otlp.endpoint=http://collector.localhost:4317",
          s"-Dotel.javaagent.enabled=$otelEnabled",
          "-Dotel.instrumentation.jetty.enabled=true",
          "-Dotel.instrumentation.common.default-enabled=true",
          "-Dotel.instrumentation.opentelemetry-api.enabled=true",
          "-Dotel.instrumentation.opentelemetry-instrumentation-annotations.enabled=true",
          s"-javaagent:$otelAgentJar",
          "-Djava.rmi.server.hostname=localhost",
          "-XX:+HeapDumpOnOutOfMemoryError"
        )
    }
  }

  object Scalac {

    val full = Seq(
      "-target:jvm-1.8",
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
      "-Ywarn-value-discard",
      "-Ywarn-unused",
      "-Ywarn-inaccessible",                            
      "-Ywarn-unused-import",
      "-Yrangepos"
    )

  }
}
