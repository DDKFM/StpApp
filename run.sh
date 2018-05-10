#!/bin/sh

java -jar StpApp.jar \
    -u $STPAPP_USER\
    -p $STPAPP_PASSWORD\
    -g $STPAPP_GROUP\
    -m $CAMPUSDUAL_UID\
    -c $CAMPUSDUAL_HASH\
    -i /cal\
    -k /chart\
    -t /times/times.txt