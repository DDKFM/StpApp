# StpApp Calendar Fetcher
The Kotlin-native implementation use the data from stpapp.ba-leipzig.de and the "Campus Dual" to generate
iCalendar-files for lectures and meals.

## required parameters
### StpApp

| Parameter       | CLI-parameter            | docker environment variable|
| ----------------|:------------------------:| --------------------------:|
| StpApp Username | --stpapp-username(or -u) | STPAPP_USER                |
| StpApp Password | --stpapp-password(or -p) | STPAPP_PASSWORD            |
| StpApp Group    | --group (or -g)          | STPAPP_GROUP               |

### CampusDual
| Parameter       | CLI-parameter            | docker environment variable|
| ----------------|:------------------------:| --------------------------:|
| CampusDual UID  | --campusdual-uid(or -m)  | CAMPUSDUAL_UID             |
| CampusDual Hash | --campusdual-hash(or -c) | CAMPUSDUAL_HASH            |

### Other parameters
| Parameter                          | CLI-parameter              | default value |
| -----------------------------------|:--------------------------:| -------------:|
| output path for *.ical-files       | --ical-output-path(or -i)  | .             |
| should the response times be logged| --withChart(or -v)         | false         |
| output path for response-time chart| --chart-output-path(or -k) | ./chart       |
| path to textfile for response-times| --times-path(or -t)        | ./times.txt   |

**--chart-output-path and --times-path are only recognized if --withChart flag is set**

By using the docker-container, the withChart-option is always true. If you want to store these files on the docker host, 
you have to mount a volume into the docker container

## Fetching-Order
1. meal events(stpapp.ba-leipzig.de)
2. all events from stpapp.ba-leipzig.de
3. all events from Campus Dual
4. compare the CampusDual and stpapp events and merge them (e.g. description from stpapp and room numbers 
from both)

# Examples
## CLI
```bash
java -jar StpApp.jar \
    --stpapp-username $STPAPP_USER \
    --stpapp-password $STPAPP_PASSWORD \
    --group $STPAPP_GROUP \
    --campusdual-uid $CAMPUSDUAL_UID \
    --campusdual-hash $CAMPUSDUAL_HASH \
    --ical-output-path ./cal \
    --withChart \
    --chart-output-path ./chart \
    --times-path ./chart/times.txt
```
## Docker
```bash
docker run --rm --name stpapp \
        -e STPAPP_USER=xxxxxx \
        -e STPAPP_PASSWORD=xxxxxx \
        -e STPAPP_GROUP=xxxxxx \
        -e CAMPUSDUAL_UID=xxxxxxx \
        -e CAMPUSDUAL_HASH=xxxxxxxxxxxxxxxxxxxxxxxxxx \
        -v /home/ddkfm/StpApp/cal/:/cal/ \
        -v /home/ddkfm/StpApp/chart/:/chart/ \
        ddkfm/stpapp
```

## Docker-Compose
```yaml
version : '3'
services:
  stpapp:
    container_name: "stpapp"
    image: ddkfm/stpapp
    environment:
      - STPAPP_USER=xxxxxx
      - STPAPP_PASSWORD=xxxxxx
      - STPAPP_GROUP=xxxxxx
      - CAMPUSDUAL_UID=xxxxxxx
      - CAMPUSDUAL_HASH=xxxxxxxxxxxxxxxxxxxxxxxxxx
    volumes:
      - /home/ddkfm/StpApp/cal/:/cal/
      - /home/ddkfm/StpApp/chart/:/chart/
```