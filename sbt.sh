#!/bin/sh
SCRIPT_DIR=`dirname $0`
echo "Script dir: $SCRIPT_DIR"
IVY_HOME=$HOME/.ivy2/
echo "Ivy home: $IVY_HOME"
java -Xmx1024M -Xss2M -Drun.mode=production -Dlogback.configurationFile=config/logback.xml -Dsbt.boot.directory="$IVY_HOME/.sbt.sh-boot" -Dsbt.global.home="$IVY_HOME/.sbt.sh" -Dsbt.home="$IVY_HOME/.sbt.sh" -Dsbt.ivy.home=$IVY_HOME/.ivy2 -Dsbt.global.staging="$IVY_HOME/.sbt.sh-staging" -Drun.mode="development" -Dquacker.configDirectoryLocation=$ -jar $SCRIPT_DIR/sbt-launch.jar "$@"
