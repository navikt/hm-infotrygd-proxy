FROM gcr.io/distroless/java21-debian12:debug
COPY build/libs/hm-infotrygd-proxy-all.jar /app.jar
ENV TZ="Europe/Oslo"
EXPOSE 8080
CMD ["/app.jar"]
