# Hoymiles

## Opendtu

[Opendtu](https://github.com/tbnobody/OpenDTU) is a Software run on an Esp32 and dose most everything.
Communication with the device, Home Assistant interface, Web Page for current Information and configuration.
And even a display if you need one. On [this](https://github.com/tost11/OpenDTU-Push-Rest-API)
fork I added the functionality to send the current data via rest to the Server application. 
So if you want or already have OpenDTU flash this fork and configure your Solar System and that's all.

## The manual Way (Ahoy)

You already have a Pi or another Microcontroller and don't want to by some new hardware (beside the needed wireless module)
Use the script implementation here it uses python to read out the data and push them to the Server.