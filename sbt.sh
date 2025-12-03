#!/bin/sh


export JAVA_HOME=/Users/jono/Library/Caches/Coursier/arc/https/github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.15%252B6/OpenJDK17U-jdk_aarch64_mac_hotspot_17.0.15_6.tar.gz/jdk-17.0.15+6/Contents/Home

SCRIPT_DIR=`dirname $0`
echo "Script dir: $SCRIPT_DIR"
IVY_HOME=$HOME/.ivy2/
echo "Ivy home: $IVY_HOME"
java -Xmx1024M -Xss2M -Drun.mode=production -Dlogback.configurationFile=config/logback.xml -Dsbt.boot.directory="$IVY_HOME/.sbt.sh-boot" -Dsbt.global.home="$IVY_HOME/.sbt.sh" -Dsbt.home="$IVY_HOME/.sbt.sh" -Dsbt.ivy.home=$IVY_HOME/.ivy2 -Dsbt.global.staging="$IVY_HOME/.sbt.sh-staging" -Drun.mode="development" -Dquacker.configDirectoryLocation=$ -jar $SCRIPT_DIR/sbt-launch.jar "$@"
