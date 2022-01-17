#!/usr/bin/python3
import ctypes
import datetime
import requests
from pymodbus.client.sync import ModbusSerialClient as ModbusClient
from pymodbus.exceptions import ModbusIOException
from pymodbus.mei_message import ReadDeviceInformationRequest
from pymodbus.constants import DeviceInformation
import time


API_ENDPOINT = "http://10.255.2.250:8080/api/solar/data/selfmade/consumption/device"
TOKEN="token: token: 623262c7-c086-43fb-8935-7e56059e6df9"
CHARGE_CONTROLLER_UNIT = 1

def getClient():
    return ModbusClient(
        method = "rtu",
        port = "/dev/tty.usbserial-AB0L19WE",
        baudrate = 115200,
        timeout = 1
    )
def current_milli_time():
    return round(time.time() * 1000)

allData=[]
while True:

    client = getClient()
    if client.connect():
        print("connect")
        result = client.read_input_registers(0x3100, 19, unit=CHARGE_CONTROLLER_UNIT)
        if isinstance(result, Exception):
            print("Got exception reading 0x3100 - 0x3118")
            print(result)
        else:
            if result.function_code < 0x80:

                data = {'timestamp':current_milli_time(),
                        'chargeVolt':result.registers[0]/100,
                        'chargeAmpere':result.registers[1]/100,
                        'batteryVoltage':result.registers[4]/100,
                        'batteryAmpere':result.registers[5]/100,
                        'batteryTemperature':result.registers[16]/100,
                        'consumptionVoltage':result.registers[12]/100,
                        'consumptionAmpere':result.registers[13]/100,
                        'deviceTemperature':result.registers[17]/100,
                        'chargerTemperature':result.registers[18]/100
                }

                # this is done by backend now
                #data['chargeWatt']=data['chargeAmpere']*data['chargeVolt']
                #data['dischargeWatt']=data['dischargeAmpere']*data['dischargeVoltage']
                #data['batteryWatt']=data['batteryAmpere']*data['batteryVoltage']


                headers = {'clientToken':TOKEN}

                allData.append(data)
                print(data)
                try:
                    for i in range(len(allData)):
                        r = requests.post(url = API_ENDPOINT,headers=headers, json =allData[0])
                        print(allData[0])
                        allData.pop(0)
                        print(r)
                except:
                    print('requests fail')
                    time.sleep(5)
                    print(allData)
                    print('sleep end')

                #when using self signed certificate
                #r = requests.post(url = API_ENDPOINT, json = data,headers=headers, verify=False)
            else:
                print("Unable to read 0x3100 - 0x3112")



        client.close()
    else:
       print("connection not possible")

    time.sleep(5)


