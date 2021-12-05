#!/usr/bin/python3
import ctypes
import datetime
import requests
from pymodbus.client.sync import ModbusSerialClient as ModbusClient
from pymodbus.exceptions import ModbusIOException
from pymodbus.mei_message import ReadDeviceInformationRequest
from pymodbus.constants import DeviceInformation
import time


API_ENDPOINT = "http://localhost:8080/api/solar/data/SelfMade"
TOKEN="ba0f9309-1c30-417e-9cda-143b5ba4452a"
CHARGE_CONTROLLER_UNIT = 1

def getClient():
    return ModbusClient(
        method = "rtu",
        port = "/dev/tty.usbserial-AB0L19WE",
        baudrate = 115200,
        timeout = 1
    )
while True:

    client = getClient()
    if client.connect():
        result = client.read_input_registers(0x3100, 19, unit=CHARGE_CONTROLLER_UNIT)
        if isinstance(result, Exception):
            print("Got exception reading 0x3100 - 0x3118")
            print(result)
        else:
            if result.function_code < 0x80:

                data = {'chargeVolt':result.registers[0]/100,
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

                print(data)
                headers = {'clientToken':TOKEN}
                r = requests.post(url = API_ENDPOINT,headers=headers, json = data)
                print(r)
                #when using self signed certificate
                #r = requests.post(url = API_ENDPOINT, json = data,headers=headers, verify=False)
            else:
                print("Unable to read 0x3100 - 0x3112")

        result = client.read_input_registers(0x311A, 2, unit=CHARGE_CONTROLLER_UNIT)
        if isinstance(result, Exception):
            print("Got exception reading 0x311A - 0x311B")
            print(result)
        else:
            if result.function_code < 0x80:
                print("Battery SOC: {}".format(result.registers[0]))
                print("Remote battery temperature: {}".format(result.registers[1]))
            else:
                print("Unable to read 0x311A - 0x311B")

        result = client.read_input_registers(0x311D, 1, unit=CHARGE_CONTROLLER_UNIT)
        if isinstance(result, Exception):
            print("Got exception reading 0x311D")
            print(result)
        else:
            if result.function_code < 0x80:
                print("Battery's real rated power: {}".format(result.registers[0]))
            else:
                print("Unable to read 0x311D")

        client.close()
    else:
       print("connection not possible")

    time.sleep(5)
