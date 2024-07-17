FROM openjdk:8
VOLUME /tmp
COPY target/*.jar sleepingFaceBi-chart.jar
ENV PORT=9095
ENTRYPOINT ["java","-jar","-Xms128m","-Xmx256m","sleepingFaceBi-chart.jar","--spring.profiles.active=prod"]
EXPOSE $PORT
