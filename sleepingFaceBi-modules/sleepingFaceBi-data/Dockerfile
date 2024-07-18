FROM openjdk:8
VOLUME /tmp
COPY target/*.jar sleepingFaceBi-data.jar
ENV PORT=9097
ENTRYPOINT ["java","-jar","-Xms128m","-Xmx256m","sleepingFaceBi-data.jar","--spring.profiles.active=prod"]
EXPOSE $PORT
