# Solar Monitroing Application
Thie repository contains a web application that receives infromation from various solar devices and shows them on a web page.

Some Features are:
- show data in graphs on web page (with live refresh)
- Login and registration for different users
- notification system (currently only mail)
- data pushes via rest
- data pushes via deye sun protorol
- support for multiple system types (home system with battery, balkony system, ...)
- customizable statuses rest endpoinds for home power managing externaly

One hosted instance is [here](https://solar.pihost.org) check out how it looks in production.

## Design
The software is split up in multiple applications and databases.

The idea is like all other solar monitoring applications/apps. The system receives data form clients (solar systems) and
provides them on a website where the user can check them via browser with graphs and all the other cool stuff.

### Databases
For databases MongoDB and InfluxDB are used. The mongoDB contains all the static information like users, systems, and permissions

The InfluxDB contains all the continuous information like, current and daily production of solar devices.

### Frontend
The [frontend](frontend) is written in React whith recharts as graph library and material-ui as ui framework. npm is used as packet manager for frontend libraries.

### Applications
Currently, there are three applications.

#### [Main Application](backend)
This spring boot application handles all the api requests (frontend requests and data pushes from clients). It checks vor permissions
and write the data then into the influx. On user requests it will fetch the data in the requested time range from the
Influx Database.

#### [Updater](updater)[main application](backend)
This spring boot applications calculates continuously combined data and checks if notifications needs to be sent if a systems goes offline.

Cannulations done in background:
- calculation of daily values (every 15min)
- calculation if system is online
- check if system is offline and send notifications

#### [Deye Connector](deye-microinverter-cloud-free)[Deye Connector](proxy)
On the deye sun inverters the connection ip and port can be changed. This application implements the basis of the backend.
It basically only proxies the information to the spring boot application via rest.

#### [Data Proxy](proxy)
Sometimes network issues or a not valid certificate stops clients from sending data to backend application. Therefore, a
proxy applications was implemented that craws current send data token from real backend and provided another data receiver for the clients.
So when the main application goes down the requests from clients can be handled on other domain (ip/location). Obviously while the main
application is down it is not possible to look into the data but the data will not be lost. After the main applications goes is
online again the proxy application wil sync them.

On the clients both domains have te be configured like a fallback. If main application is not reachable try second one.

### Scaling

## Clients

There are a lot of different solar devices out there and everyone has a different way of reaching the data. Some possible
ways are described in the [client](client) folder.

## Local Setup

This section describes how to run a local instance of the application(s) for new implementations and debug purpose.

### Environment

The environment cann be started with the environment docker file from base folder.

```bash
docker-compose -f docker-compose-env.yml up
```

It stars the mongodb and influxdb with local users and passwords. To find them out look into compose-env file.

### Main Applications

#### Frontend
The frontend can be run via npm commands. Therefor change context of terminal to [frontend](frontend) folder.

- Setup: npm i
- Run dev Port: npm run dev
- Build application: npm run build
- Build dev application: npm run build-dev

#### Backend

The spring boot backend loads users and passwords from application-local.yml (have to be same as in docker-compose env file).
To use this local yaml the profile have to best to 'local'

It is possible to run additional profile 'debug'. This is crate DebugService class that crates test user with system. Also,
some debug data will be generated that will be continuously send. Test username: debug, password: testtest.

### Updater
The spring boot proxy loads users and passwords from application-local.yml (have to be same as in docker-compose env file).
To use this local yaml the profile have to best to 'local'

### Proxy
The spring boot proxy loads users and passwords from application-local.yml (have to be same as in docker-compose env file).
To use this local yaml the profile have to best to 'local'

### Deye Connector
The Deye Connector is a [sub-repository](https://github.com/tost11/deye-microinverter-cloud-free) that is forked of the [Deye Connector - Cloud Free Repository](https://github.com/Hypfer/deye-microinverter-cloud-free).

The application can be run via npm commands. Therefor change context of terminal to [deye-microinverter-cloud-free/dummycloud](deye-microinverter-cloud-free/dummycloud)[deye-microinverter-cloud-free](deye-microinverter-cloud-free) folder.

- Setup: npm i
- Run dev Port: npm run start

## Endpoint definitions

### Data Endpoints
There are two endpoints. One for only one data sample and another for Multiple data samples.

- Url for one Sample: **{PROTOCOL}://{HOST}:{PORT}/api/solar/data?systemId=[ID]**
- Url for multiple Sample: **{PROTOCOL}://{HOST}:{PORT}/api/solar/data/mult?systemId=[ID]**

Additional required information.
- id as request parameter, so the application knows the System the data is for.
- ClientToken in the header, so the application authenticates the request.


### DTOs
To send Date for Systems there ist only one DTO object but most of the parameters are optional.
Some attributes are multiple times available. The higher ones override the lower ones and the lower ones are
calculated by the higher ones. That means if on a sample input, output or battery values are set the device
and samples values are also set. But when values are also set on device or sample these are taken as values
for device or total data.

#### Examples
- Input, Output, Batteries Sample: [here](example-data/input_output_battery_sample.json)
- Device Sample: [here](example-data/device_sample.json)
- Base Sample: [here](example-data/sample.json)

## Running your own instance

For deploying and running the application(s) the file [docker-compose-deploy.yml](docker-compose-deploy.yml) is provided.
It will run all services.

```bash
docker-compose -f docker-compose-deploy.yml up -d
```

#### Environment Variables
In the file the attributes can be changed as needed.
The following values should be changed:

| Variable            | Description                               |
|:--------------------|-------------------------------------------|
| MONGO_PASSWORD      | password for the mongodb                  |
| INFLUX_PASSWORD     | password for the influxdb                 |
| INFLUX_ADMIN_TOKEN  | influxdb admin token                      |
| DEYE_API_TOKEN      | token for the deye connector to push data |
| ENVIRONMENT_JWT_KEY | jtw key                                   |

#### Volumes
For mongo and influxdb the volume mount should be changed from /temp/solar to something more lasting.

#### Mail Notification
To enable mail notifications uncomment the mail configuration and fill out spring mail configuration as needed.

### Debugging
To debug the deployed application it is possible to also export the database port by adding [docker-compose-deploy-access.yml](docker-compose-deploy-access.yml)
to the execution command.

```bash
docker-compose -f docker-compose-deploy.yml -f docker-compose-deploy-access.yml up -d
```

### Initial Setup
After successfully deploying the application create a user with the UI and change the admin user flag int the
database to true. After that as page setting can be done with this account.

## Application Behavior

### Types
There are multiple types of solar systems. These do not change API and data handling it will only have affect
on the shown frontend graphs. The types represent for example solar sysems with or without battery, island symstems or
systems with a connection to the local grid system.

### Public Mode
The public mode allows the user to share the solar data with the world, so any person cann see the current values.

| Mode       | Visibility                                                                                 |
|:-----------|--------------------------------------------------------------------------------------------|
| None       | Systems is only visible for permitted users                                                |
| Production | Only the input values are visible, not the used power so no one can gues that you are home |
| All        | All data are public available                                                              |

### Permission management
It is possible to parmit other users to see or edit your solar system. The permissions are:

| Permission |Possibilities|
|:-----------|-------------|
| View       |This user is permitted to see all valies|
| Manage     |This user is permitted to change settings on this system|
| Admin      |This user is permiteed to change settings,permissions and delete the system|

### Tags
It is possible to tag systems and use the tags for searching. Also tags with "start page setting" will be grouped and shown
on the start page. Tags with the "admin setting" can only set by admins.

### Status
It is possible to add custom status to a systems. These are actually only bool values that can be toggled and viewed on website and
by API requests. They are suppoed to be used by services from your home to manually enable or disable electrical loads from remote.

The api endpoint is: **{PROTOCOL}://{HOST}:{PORT}/api/status/[ID]**

For authentication the push API key is used.