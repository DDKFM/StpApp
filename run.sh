#!/bin/sh

java -jar StpApp.jar \
    --stpapp-username $STPAPP_USER \
    --stpapp-password $STPAPP_PASSWORD \
    --group $STPAPP_GROUP \
    --campusdual-uid $CAMPUSDUAL_UID \
    --campusdual-hash $CAMPUSDUAL_HASH \
    --ical-output-path /cal \
    --withChart \
    --chart-output-path /chart \
    --times-path /chart/times.txt