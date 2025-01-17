ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

ThisBuild / libraryDependencies ++= Seq(
	// later version of ASM for object classpath scanning
	"org.clapper" %% "classutil" % "1.5.1",
	"org.ow2.asm" % "asm" % "9.2"
)