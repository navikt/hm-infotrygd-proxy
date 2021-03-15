FROM navikt/java:15
COPY init.sh /init-scripts/init.sh
COPY /build/libs/hm-infotrygd-proxy-fat-1.0-SNAPSHOT.jar app.jar
