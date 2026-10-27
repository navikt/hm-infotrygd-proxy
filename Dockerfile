FROM gcr.io/distroless/java21-debian13:nonroot
WORKDIR /app
COPY build/libs/hm-infotrygd-proxy-all.jar app.jar
ENV TZ="Europe/Oslo"
EXPOSE 8080
CMD ["./app.jar"]
