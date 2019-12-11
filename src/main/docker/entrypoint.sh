#!/bin/bash
set -e
if [ -d "/run/secrets" ]
then
    secrets=`find  /run/secrets/ -maxdepth 1 -type f ! -name "*FILE"  -exec basename {} \;`
    for s in $secrets
    do
        echo "set env $s"
        export "$s"="$(cat /run/secrets/$s)"
    done
fi

if [ -n "${APPLICATION_EXTERNAL_CLASSPATH}" ]; then
    echo "
    Found external application classpath ${APPLICATION_EXTERNAL_CLASSPATH}
    app.war will be modified
    Next libs found in external classpath:
    $(ls ${APPLICATION_EXTERNAL_CLASSPATH})
    "
    mkdir /tmp/app
    unzip -qq app.war -d /tmp/app
    cp -vR ${APPLICATION_EXTERNAL_CLASSPATH}/* /tmp/app/WEB-INF/lib
    cd /tmp/app
    zip -r -0 -q - . > /app.war
    rm -rf /tmp/app
    cd /
fi

echo "The application will start in ${JHIPSTER_SLEEP}s..." && sleep ${JHIPSTER_SLEEP}
exec java ${JAVA_OPTS} -Xmx$XMX -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom -jar "app.war" "$@"
