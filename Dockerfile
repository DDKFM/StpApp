FROM anapsix/alpine-java

COPY target/StpApp-1.9-jar-with-dependencies.jar /app/StpApp.jar
COPY run.sh /app/run.sh
COPY chart_template.html /app/chart_template.html

RUN mkdir /cal
RUN mkdir /chart

WORKDIR /app

CMD ["sh", "run.sh"]
