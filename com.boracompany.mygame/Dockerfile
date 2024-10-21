FROM fedora:latest

RUN dnf update -y && \
    dnf install -y java-17-openjdk libXext libXrender libXtst libXi openjfx Xvfb

WORKDIR /app
COPY /target/*-jar-with-dependencies.jar /app/app.jar
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
