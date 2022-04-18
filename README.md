# What is that here ?
This is an application for receiving continuesly information from solar systems and showing them on a webside, 
with graphs, historical information and all the other cool information stuff the system reports.

Also there is some user permission management and some tested readings scripts for specific devices.

## Why another solar monitoring application
I just couldn't find one that fits my recommendations

# The application

## how to use
Way one Clone the code and run the application yourself.
Second way register on my running instance and use that one (with some limitations)

Create an account and create a new Solar System of the type you need.
Copy generated push token and install one of the push scripts on your reading device and insert token and endpoint
Switch to the dashboard side and enjoy the graphs and shown information.

### System Types

there are different types possible solar systems

They differ by shown values and needed data on the push endpoints

### The simple Types
A system that only contains charging values v.e. current power in watt, ampere and voltage

### The selfmade Types
A system combined with a battery. Supported values here are Panel Charge values, 
battery charge and discharge values, consumption discharge and even inverter discharge values if available

### The grid Types
A system powering, powering the local power grid. Supported values are charge values and discharge values on grid.
Also subdevices are possible for supporting a system with mulltiple input strings.

#### Grid Type with battery
to be implemented

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

## Push endpoints

For pushing data it is necessary to have an access token for the specific system  and push against the
correct endpoint for the specific system. The access token os shown on system creation but can be regenerated
on the settings page of the system.

## Client Scripts

### Epever
### Victon
### SMA

#Implementation

## Databases
For the historical information influx is used. The user and permission information are stored in neo4j.

##Backend
The backend uses Spring Boot.
It handles incoming solar data requests and stores tem in the database.
Also web requests form browsers are handled and influx querys are generated and send against the database.
Then the result is formatted and send back to the client.

##Frontend
The frontend is typescript with react. For the graphs the library recharts is used.

##Web Authorization
The web authorization is done by jwt token stored in the browser cookie

#Local setup section
Todo -> how to start docker-compose files

#My implementation (live example)
Todo -> link to webisode, also some screenshots from application and new front page for application

