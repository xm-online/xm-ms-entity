FROM adoptopenjdk:11-jre-hotspot

ENTRYPOINT ["/entrypoint.sh"]

ADD src/main/docker/entrypoint.sh /

RUN chmod 755 entrypoint.sh && apt-get update && apt-get install -y locales locales-all zip unzip curl && apt-get clean

ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS \
    JHIPSTER_SLEEP=0 \
    XMX=256m \
		TZ=Europe/Kiev \
		LANG=en_US.UTF-8 \
		LANGUAGE=en_US:en \
		TERM=xterm \
    JAVA_OPTS="-Xms64m -Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.ssl=false \
               -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.local.only=false \
               -Dcom.sun.management.jmxremote.rmi.port=19999 -Dcom.sun.management.jmxremote.port=19999 \
               --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED"
# --add-opens skip warning for groovy lib, see https://issues.apache.org/jira/browse/GROOVY-8339
# add directly the war
ADD build/libs/*.war /app.war

EXPOSE 8081
