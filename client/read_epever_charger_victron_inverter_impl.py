#!/usr/local/bin/python3 -u

from read_epever_charger_impl import readCharger
from read_victron_inverter_impl import readInverter
from send_to_kafka import MyKafkaProducer
from local_data_storage import MyDatabase
from datetime import datetime
import time
import json
import signal
import os
import time, threading

EPEVER_LOADER_PORT = "/dev/ttyUSB1"
VICTRON_INVERTER_PORT = "/dev/ttyUSB0"
POLL_TIME = 5.
TOPIC = "new-solar-event"

LARGE_RESEND_INDEX = -1

running = True

#for my setup
k = MyKafkaProducer(["raspberrypi-kafka-1","raspberrypi-kafka-2"],"raspberrypi-solar")

d = MyDatabase("data.db")

def signal_handler(sig, frame):
  global running
  print('You pressed Ctrl+C!')
  if running is False:
    print('hard termination because second try')
    os._exit(1)
  running = False

signal.signal(signal.SIGINT, signal_handler)

def multWithNone(v1,v2,name):
  if v1 is None and v2 is None:
    return None
  if v1 is None:
    return v2[name]
  if v2 is None:
    return v1[name]
  return v1[name] + v2[name]

def oneNotEmptyWithNone(v1,v2,name):
  if v1 is not None:
    return v1[name]
  if v2 is not None:
    return v2[name]
  return None

def resendMissingData():
  print("Resend missing data")
  global k
  #if k.isConnected() is True:
  NUM = 10
  more = True
  global LARGE_RESEND_INDEX
  LARGE_RESEND_INDEX = LARGE_RESEND_INDEX + 1
  if(LARGE_RESEND_INDEX == 10):
    LARGE_RESEND_INDEX = 0
  while True:
    enties = d.getEntries(NUM,0)
    if len(enties) == 0:
      break
    print("Try sending",len(enties),"missing Data")
    results = [None] * NUM
    i = 0
    for e in enties:
      results[i] = k.sendMessage(TOPIC,e.data)
      i = i +1
      #print(e.data)
    i = -1
    k.flush()
    for e in enties:
      i = i + 1
      r = results[i]
      if r is None:
        print("Could not send message because kafka is not connected")
        continue
      try:
        r.get()
      except Exception as ex:
        print("Could not send message because kafka send future timed out")
        continue
      d.removeEntry(e.id)
      print("Succesfull resend entry ",e.id)
    if LARGE_RESEND_INDEX == 0:
      break
  #else:
  #  print("no resending data because kafka isnt connected")
  print("Staring new wait for resend missing data")
  threading.Timer(5, resendMissingData).start()

def readChargerAndInverter():

  out1 = readCharger(EPEVER_LOADER_PORT)
  out2 = readInverter(VICTRON_INVERTER_PORT)

  out = {}
  if out1 is not None:
    out['chargeVoltage']=out1['chargeVoltage']
    out['chargeAmpere']=out1['chargeAmpere']
    out['chargeWatt']=out1['chargeWatt']
    out['batteryVoltage']=out1['batteryVoltage']
    out['batteryAmpere']=out1['batteryAmpere']
    out['batteryWatt']=out1['batteryWatt']
    out['consumptionVoltage']=out1['consumptionVoltage']
    out['consumptionAmpere']=out1['consumptionAmpere']
    out['consumptionWatt']=out1['consumptionWatt']
    out['deviceTemperature']=out1['deviceTemperature']
    out['batteryTemperature']=out1['batteryTemperature']

  if out2 is not None:
    out['consumptionInverterVoltage']=out2['consumptionInverterVoltage']
    out['consumptionInverterAmpere']=out2['consumptionInverterAmpere']
    out['consumptionInverterWatt']=out2['consumptionInverterWatt']
    out['inverterTemperature']=out2['inverterTemperature']

  if out1 is None and out2 is None:
    return None

  out['totalConsumption'] = multWithNone(out1,out2,"totalConsumption")
  out['batteryWatt'] = multWithNone(out1,out2,"batteryWatt")
  out['batteryVoltage'] = oneNotEmptyWithNone(out1,out2,"batteryVoltage")
  out['batteryAmpere'] = out['batteryWatt'] / out['batteryVoltage']

  out['timestamp'] = round(time.time() * 1000)
  out['duration'] = POLL_TIME

  #this is just for my setup
  out['location'] = "home"
  out['device'] = "raspberrypi-solar"

  return out


resendMissingData()

stamp = datetime.now()

while running:
  out = readChargerAndInverter()

  if out is not None:
    jsonStr = json.dumps(out)
    d.addEntry(jsonStr)
  else:
    print("out is zero so no data can be send")

  now = datetime.now()
  dif = now - stamp

  timeToSleep = POLL_TIME - dif.total_seconds()
  print("sleeptime is: ",timeToSleep)
  if(timeToSleep > 0):
    print("Sleep for: ", timeToSleep, " Seconds")
    time.sleep(timeToSleep)
  stamp = datetime.now()

os._exit(0)