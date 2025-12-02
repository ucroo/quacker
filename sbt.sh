#!/bin/sh
# Use Java 11 for sbt 1.x
export JAVA_HOME="$HOME/Library/Caches/Coursier/arc/https/github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.29%252B7/OpenJDK11U-jdk_aarch64_mac_hotspot_11.0.29_7.tar.gz/jdk-11.0.29+7/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# Check if sbt is installed via Homebrew or use a direct download
if command -v sbt >/dev/null 2>&1; then
  sbt -Drun.mode=development -Dlogback.configurationFile=config/logback.xml -Dmetlx.configurationFile=config/application.xml -Dquacker.configDirectoryLocation=config"$@"
else
  echo "sbt not found. Installing via Homebrew..."
  brew install sbt
  sbt -Drun.mode=development -Dlogback.configurationFile=config/logback.xml -Dmetlx.configurationFile=config/application.xml -Dquacker.configDirectoryLocation=monitoringDashboardConfig "$@"
fi
