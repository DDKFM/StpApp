# StpApp Calendar Fetcher
The Kotlin-native implementation uses the stpapp.ba-leipzig.de URL and the "Campus Dual" to generate
vCard-Files for lectures and meals.


# CLI
```bash
java -jar StpApp.jar \
    -u $STPAPP_USER\
    -p $STPAPP_PASSWORD\
    -g $STPAPP_GROUP\
    -m $CAMPUSDUAL_UID\
    -c $CAMPUSDUAL_HASH\
    -i /cal\
    -k /chart\
    -t /times/times.txt
```
# Docker
```bash
docker run --rm --name stpapp \
        -e STPAPP_USER=xxx \
        -e STPAPP_PASSWORD=xxx \
        -e STPAPP_GROUP=xxxxxx   \
        -e CAMPUSDUAL_UID=    \
        -e CAMPUSDUAL_HASH=    \
        -v /home/ddkfm/docker/StpApp/cal/:/cal/ \
        -v /home/ddkfm/docker/StpApp/chart:/chart/ \
        -v /home/ddkfm/docker/StpApp/times.txt/:/times/times.txt 
        ddkfm/stpapp
```

# Docker-Compose
```yaml
version : '3'
services:
  stpapp:
    container_name: "stpapp"
    image: ddkfm/stpapp
    environment:
      - STPAPP_USER=xxx
      - STPAPP_PASSWORD=xxx
      - STPAPP_GROUP=xxxxxx
      - CAMPUSDUAL_UID=xxxxxx
      - CAMPUSDUAL_HASH=xxxxxxxxxxxxxxxxxxxxxxxxxxxx
    volumes:
      - /home/ddkfm/docker/StpApp/cal/:/cal/
      - /home/ddkfm/docker/StpApp/chart/:/chart/
      - /home/ddkfm/docker/StpApp/times.txt:/times/times.txt
```
