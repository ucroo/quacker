FROM jetty:11.0.20-jdk17-alpine-amazoncorretto
USER root
RUN apk add --no-cache iputils
USER jetty
EXPOSE 8080
ENV JAVA_OPTIONS="--add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED"
COPY server.ini /var/lib/jetty/start.d/server.ini
COPY *.war /var/lib/jetty/webapps/root.war