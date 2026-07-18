FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home --home-dir /home/giftchat --shell /usr/sbin/nologin giftchat
COPY --from=build /app/target/gift-chat-server-0.0.1-SNAPSHOT.jar app.jar
COPY docker-entrypoint.sh /usr/local/bin/gift-chat-entrypoint.sh
RUN chmod 755 /usr/local/bin/gift-chat-entrypoint.sh
RUN chown -R giftchat:giftchat /app
USER root
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8081
ENTRYPOINT ["/usr/local/bin/gift-chat-entrypoint.sh"]
