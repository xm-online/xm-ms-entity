FROM jbsukraine/oracle-jre:8

ENV XMX=1024m
ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS \
    JHIPSTER_SLEEP=0 \
    JAVA_OPTS="-Xmx$XMX -Xms128m -Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.rmi.port=19999 -Dcom.sun.management.jmxremote.port=19999"

# add directly the war
ADD build/libs/*.war /app.war

EXPOSE 8081 5701/udp
CMD echo "The application will start in ${JHIPSTER_SLEEP}s..." && \
    sleep ${JHIPSTER_SLEEP} && \
    exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar /app.war
