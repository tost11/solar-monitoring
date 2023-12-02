# What is that here ?
This is an application for receiving continuesly information from solar systems and showing them on a webside, 
with graphs, historical information and all the other cool information stuff the system reports.

Also there is some user permission management and some tested readings scripts for specific devices.

## Why another solar monitoring application
I just couldn't find one that fits my recommendations

## Where is the application
To check out the look or register "if possible sometime" look [here](https://solar.pihost.org)

# The application

## how to use
Way one Clone the code and run the application yourself.
Second way register on my running instance and use that one (with some limitations)

Create an account and create a new Solar System of the type you need.
Copy generated push token and install one of the push scripts on your reading device and insert token and endpoint
Switch to the dashboard side and enjoy the graphs and shown information.

### System Types
There are different Solar System types.
They differ by shown values on Website and config options.

### The Very Simple Type
A system that only contains charging watt

### The Simple Types
A system that only contains charging values v.e. current power in watt, ampere and voltage

### The selfmade Types
A system combined with a battery, with all possible Values, v.e. AC,DC input and AC,DC output. Also current battery status.

### The grid Type
A system, powering the local power grid. Supported values are charge values and discharge values on grid.

### The grid Battery Type
A system, powering the local power grid, with battery for own consumption. Supported values are charge values and discharge values on grid.

## Access management
On the system page it is possible to set and change permissions for other users

### View permissions
You like to share your solar system information with other persons or only host a dashboard to show it anywhere.
Create a second account and give that one view permissions on your system. The "view account" will only 
have access to view the dashboards and nothing else.

### Edit permissions
Allows the user to change all system information and generate a new data push token.

### Admin permissions
Allows the user to also perform permissions changes on a system

## Pushing Data
Data is send to System by continuous rest requests.

### Endpoints
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

### Access Token
For pushing data it is necessary to have an access token for the specific system.
The access token is shown on system creation but can be regenerated on the settings page of the system.
The token must be set in Rest Request Http Header with name: **clientToken**

## Client Scripts
While the documentation (and the scripts) are not finished you can checkout the existing test scripts [here](tree/develop/client)

# Implementation

## Databases
For the historical information influx is used. The user and permission information are stored in mongo.

## Backend
The backend uses Spring Boot.
It handles incoming solar data requests and stores tem in the database.
Also web requests form browsers are handled and influx querys are generated and send against the database.
Then the result is formatted and send back to the client.

## Frontend
The frontend is typescript with react. For the graphs the library recharts is used.

## Web Authorization
The web authorization is done by jwt token stored in the browser cookie

# Local setup section
Todo -> how to start docker-compose files

