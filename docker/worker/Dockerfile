FROM frolvlad/alpine-oraclejdk8:slim
COPY simple-worker/build/install/simple-worker /opt/

ENTRYPOINT ["/opt/bin/simple-worker", "start"]