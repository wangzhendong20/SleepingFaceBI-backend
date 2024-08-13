FROM openjdk:8
VOLUME /tmp
COPY target/*.jar sleepingFaceBi-task.jar
ENV PORT=9098
ENTRYPOINT ["java","-jar","-Xms128m","-Xmx256m","sleepingFaceBi-task.jar","--spring.profiles.active=prod"]
EXPOSE $PORT
