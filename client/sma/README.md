
## Yasi

Yasdi is a serial connection Interface that allows you to check loader status of SMA devices. Last update is from 2012 and original it is a c Implementation
but it also comes with a CommandlineInterface, that I used for my implementation. As I understand correct it supports connecting the SunyBoys directly via
modbus rs485 and the controller via rs232. (I only tested rs323 controller)

There are some Python wrapper implementations in the web but none of them worked for me, so I implemented my own by using Python subcommand library
to parse the interactive shell. I suppose it was never designed to use it that way :P

However, the Implementation is [here](yasdi.py), you have to change some parameters in the code for now. (TODO make it more comfortable, but for now that's the way)
Before to use it you have to install Yasdi. Have a look [here](https://www.sma.de/en/products/apps-software/yasdi). For information how to compilation look into the Readme in the Source of Yasdi.
