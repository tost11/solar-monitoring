# Push Clients

Here are now described multiple ways to read the data from the devices.
In every folder is one read.py, read_inverter.py or read_charger.py Script that is a dump script that only reads the data and send them to the Server.
The scripts with database in them store the collected Data in a sqlite Database while no Internet connection is available
so no data went missing. The [read_all.py](read_all.py) scripts allows you to collect data form multiple devices
and types (hoymiles,epever,victron) and combines them in one request.

## Epever

These devices are some sort for cheap but also do their job. Fore check [here](epever)

## Victon

Is one of the most known provider for Island and camping Solar system.
Also they have a large number of Community projects. [Here](victron) is mine Implementation.

## SMA

SMA has implemented different ways to read out device data over the Years.
And all of them are different in the sort the date is represented and the way its red out.

Some of them are found out and Implemented check them out [here](sma)

## Hoymiles

The hoymiles are grid Mini-pv systems. They communicate via 2.4Gz Wireless.
The most common Projects are [Ahoy](https://github.com/lumapu/ahoy) and [OpenDTU](https://github.com/tbnobody/OpenDTU) I modified both a bit so they match my requirements.
Check them out [here](hoymiles)

