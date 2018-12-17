#!/usr/bin/env bash
secrets=`ls /run/secrets/ 2>/dev/null |egrep -v '.*_FILE$'`
for s in $secrets
do
    echo "set env $s"
    export "$s"="$(cat /run/secrets/$s)"
done

if [ -n "${APPLICATION_DATASOURCE_EXTERNAL_DRIVER}" ]; then
    echo "Found external database driver ${APPLICATION_DATASOURCE_EXTERNAL_DRIVER} app.war will be modified"
    mkdir /tmp/app
    unzip -qq app.war -d /tmp/app
    cp ${APPLICATION_DATASOURCE_EXTERNAL_DRIVER} /tmp/app/WEB-INF/lib
    cd /tmp/app
    zip -r -0 -q - . > /app.war
    rm -rf /tmp/app
    cd /
fi

echo "The application will start in ${JHIPSTER_SLEEP}s..." && sleep ${JHIPSTER_SLEEP}
exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar "app.war" "$@"
