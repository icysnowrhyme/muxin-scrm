FROM eclipse-temurin:17-jre

LABEL maintainer="MUXIN"

ENV TZ=Asia/Shanghai

VOLUME /tmp

ADD target/muxin-scrm.jar app.jar

EXPOSE 8001

CMD ["java", "-Xms512m", "-Xmx512m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app.jar"]
