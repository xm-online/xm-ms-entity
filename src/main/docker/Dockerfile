FROM jbsukraine/oracle-jre:8.202

ENV XMX=1024m \
    SPRING_OUTPUT_ANSI_ENABLED=ALWAYS \
    JHIPSTER_SLEEP=0 \
    JAVA_OPTS="-Xms128m -Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.rmi.port=19999 -Dcom.sun.management.jmxremote.port=19999"

EXPOSE 8081 5701/udp

ENTRYPOINT ["/entrypoint.sh"]

RUN apt-get update \
        && apt-get install -y --no-install-recommends --no-install-suggests zip unzip \
        && rm -rf /var/lib/apt/lists/*

ADD src/main/docker/entrypoint.sh /

RUN chmod 755 entrypoint.sh

ADD build/libs/*.war /app.war
