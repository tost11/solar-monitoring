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

The idea is like all other solar monitoring applications/apps. The backend receives data form clients (solar systems) and
provides them on a websiede where the enduser can check them via browser with with graphs and all the other cool stuff.

### Databases
For databases MongoDB and InfluxDB are used. The mongoDB contains all the static information like users, systems, and permissions

The InfluxDB contains all the continous information like, current and daily production of solar devices.

### Frontend
The [frontend](frontend) is written in React wich rechar as graph library and material-ui as ui framework. Also npm is used as packet manager for frontend libraries.

### Applications
Currently there are three applicatoions.

#### [Main Application](backend)
This spring boot application handles all the api requrests (frontend reqeusts and data pushes from clients). It checks vor permissions
and write the data then into the influx. On enduser requests it will fetch the data in the requested time range from the
Influx Database.

#### [Updater](updater)[main application](backend)
This spring boot applications calculates continously combined data and checks if notifications needs to be send if a systems goes offline.

Calucations done in background:
- calucation of daily values (every 15min)
- calcuation if system is online
- check if system is offline and send notifications

#### [Deye Connector](deye-microinverter-cloud-free)[Deye Connector](proxy)
On the deye sun inverters the connection ip and port can be changed. This application implements the bascis of the backend.
It basicly only proxies the invromation to the spring boot application via rest.

#### [Data Proxy](proxy)
Some times network issues or a not valid certificate stops clients from sending data to backend application. Therefore a
proxy applications was implemented that craws current send data token from real backend and provieds another data receiver for the clients.
So when the main applictaion goes down the requersts from clients can be handled on aother domain (ip/location). Obiously while the main
applictaion is down it is not possible to look into the data but the data will not be lost. After the main applications goes is
online again the proxy application wil sync them.

On the clients both domains have te be configured like a fallback. If main application is not reachable try second one.

### Scaling

## Clients

There are alot of different solar devices out there and everyohne has a different way of reaching the data. Some possible
ways are described in the [client](client) folder.

## Local Setup

This section describes how to run a local instance of the applictaion(s) for new implementations and debug purpose.

### Environent

The enviroment cann be started with the enviroment docker file from base folder.

```bash
docker-compose -f docker-compose-env.yml up
```

It stars the mongodb and influxdb with local users and passowords. To find them out look into compose-env file.

### Main Applications

#### Frontend
The frontend can be run via npm commands. Therefor change contect of terminal to frontend folder.

- Setup: npm i
- Run dev Port: npm run dev
- Build application: npm run build
- Build dev application: npm run build-dev

#### Backend

The spring boot backend loads users and passowrds from application-local.yml (have to be same as in docker-compose env file).
To use this local yaml the profile have to best to 'local'

Also it is possible to run additional profile 'debug'. This is crate DebugService class that crates test user with system. Also
some debug data will be genarated that will be contiously send. Test username: debug, password: testtest.

### Updater
The spring boot proxy loads users and passowrds from application-local.yml (have to be same as in docker-compose env file).
To use this local yaml the profile have to best to 'local'

### Proxy
The spring boot proxy loads users and passowrds from application-local.yml (have to be same as in docker-compose env file).
To use this local yaml the profile have to best to 'local'

### Deye Connector
//TODO find out

## Endpoint definitions

### Data Endpoints
There are two endpoints. One for only one data sample and another for Multiple data samples.

- Url for one Sample: **{PROTOCOL}://{HOST}:{PORT}/api/solar/data?systemId=[ID]**
- Url for multiple Sample: **{PROTOCOL}://{HOST}:{PORT}/api/solar/data/mult?systemId=[ID]**

The only parameter is the System id, so the application knows the System the data is for.

### DTOs
To send Date for Systems there ist only one DTO object but most of the parameters are optional.
Some attributes are multiple times available. The higher ones override the lower ones and the lower anes are
calculated by the higher ones. That means if on a sample input, output or battery values are set the device
and samples values are also set. But when values are also set on device or sample these are taken as values
for device or total data.

#### Examples
- Input, Output, Batteries Sample: [here](example-data/input_output_battery_sample.json)
- Device Sample: [here](example-data/device_sample.json)
- Base Sample: [here](example-data/sample.json)

## Running your own instance

### Environment variables

### Initial Setup

## Application Behavior

### Types

### Permision management

### Tags

### Status