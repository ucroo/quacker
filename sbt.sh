#!/bin/sh

sbt -Dlogback.configurationFile=appConfig/logback.xml -Dmetlx.configurationFile=appConfig/application.xml -Dquacker.configDirectoryLocation=monitoringDashboardConfig  "$@"
