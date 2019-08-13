# 本文件由gmt-meta-scan于 2018-07-09 16:08:02 自动生成
# 可以通用的Dockerfile
FROM openjdk:11.0.2-alpine

RUN mkdir /app

WORKDIR /app

CMD java $JAVA_OPTS -jar -server /app/app.jar

COPY ./build/libs/*.jar /app/app.jar