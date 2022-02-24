#!/usr/bin/python3
import ctypes
from datetime import datetime
import requests
from pymodbus.client.sync import ModbusSerialClient as ModbusClient
from pymodbus.exceptions import ModbusIOException
from pymodbus.mei_message import ReadDeviceInformationRequest
from pymodbus.constants import DeviceInformation
import time
import sys
import os
import traceback

n = len(sys.argv)


API_ENDPOINT = "http://localhost:8080/api/solar/data/selfmade/consumption/device/mult?systemId=5"
CHARGE_CONTROLLER_UNIT = 1
POLL_TIME = 10

def getClient():
    return ModbusClient(
        method = "rtu",
#        port = "/dev/tty.usbserial-AB0L19WE",
        port = "/dev/ttyUSB0",
        baudrate = 115200,
        timeout = 1
    )
def current_milli_time():
    return round(time.time() * 1000)

timeToSleep=POLL_TIME
allData=[]
if n>1:
    TOKEN=sys.argv[1]
else:
    print("enter a Token")
    TOKEN=input ("")

while True:
    print("\n\nCheck for data")

    stamp = datetime.now()

    client = getClient()

    if client.connect():
        print("connect")
        result = client.read_input_registers(0x3100, 19, unit=CHARGE_CONTROLLER_UNIT)
        if isinstance(result, Exception):
            print("Got exception reading 0x3100 - 0x3118")
            print(result)
        else:
            if result.function_code < 0x80:

                data = [{'timestamp':current_milli_time(),
                        'duration':POLL_TIME/100,
                        'chargeVolt':result.registers[0]/100,
                        'chargeAmpere':result.registers[1]/100,
                        'batteryVoltage':result.registers[4]/100,
                        'batteryAmpere':result.registers[5]/100 - result.registers[13]/100,
                        'batteryTemperature':result.registers[16]/100,
                        'consumptionVoltage':result.registers[12]/100,
                        'consumptionAmpere':result.registers[13]/100,
                        'deviceTemperature':result.registers[17]/100,
                        'chargerTemperature':result.registers[18]/100
                }]

                # this is done by backend now
                #data['chargeWatt']=data['chargeAmpere']*data['chargeVolt']
                #data['dischargeWatt']=data['dischargeAmpere']*data['dischargeVoltage']
                #data['batteryWatt']=data['batteryAmpere']*data['batteryVoltage']


                headers = {'clientToken':TOKEN}

                allData.append(data)
                print(data)
                try:
                    for i in range(len(allData)):
                        r = requests.post(url = API_ENDPOINT,headers = headers, json = allData[0])
                        print(r)
                        if r.status_code == 200:
                            print(allData[0])
                            allData.pop(0)
                        elif r.status_code == 401:
                            print("Token not exist")
                            os._exit(1)
                        else:
                            raise
                except:
                    print('requests fail')
                    print(allData)
                    print(traceback.format_exc())


                #when using self signed certificate
                #r = requests.post(url = API_ENDPOINT, json = data,headers=headers, verify=False)
            else:
                print("Unable to read 0x3100 - 0x3112")



        client.close()
    else:
       print("connection not possible to solar system")

    dif = datetime.now() - stamp
    timeToSleep = POLL_TIME - dif.total_seconds()
    if(timeToSleep > 0):
        print("Sleep for: ", timeToSleep, " Seconds")
        time.sleep(timeToSleep)


