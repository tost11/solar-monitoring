#!/usr/local/bin/python3 -u

from read_victron_charger_impl import VictronCharger
from read_victron_inverter_impl import VictronInverter
from read_hoymiles import HoymilesCharger
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

#INIT_VICTRON_CHARGERS = [VictronCharger("/dev/ttyUSB1","Loader 1"),VictronCharger("/dev/ttyUSB2","Loader 2"),VictronCharger("/dev/ttyUSB3","Loader 3")]
INIT_VICTRON_CHARGERS = []
#INIT_VICTRON_INVERTERS = [VictronInverter("/dev/ttyUSB0","Inverter 1")]
INIT_VICTRON_INVERTERS = []
INIT_HOYMILES = [HoymilesCharger(114182110459,"Hoymiles")]

# TODO also implemente victronloaders
#INIT_EPEVER_LOADERS = []
#INIT_EPEVER_INVERTERS = []

#this here is for my setup
k = MyKafkaProducer(["raspberrypi-kafka-1","raspberrypi-kafka-2","raspberrypi-kafka-3"],"raspberrypi-solar")


d = MyDatabase("data.db")

def signal_handler(sig, frame):
  global running
  print('You pressed Ctrl+C!')
  if running is False:
    print('hard termination because second try')
    os._exit(1)
  running = False

signal.signal(signal.SIGINT, signal_handler)

def sumArrWithNone(arr,name,device=False):
  ok = 0
  res = None
  for obj in arr:
    if obj is None or not name in obj or obj[name] is None:
      continue
    ok = ok + 1
    if res is None:
      res = obj[name]
    else:
      res = res + obj[name]

  if res is not None and device == True:
    res = res / ok

  return res

def sumByWight(arr,nameSum,nameWight,givenTotal=None):
  if(len(arr) == 0):
    return None

  for i in range(len(arr)):
    if arr[i] is None or not nameSum in arr[i] or arr[i][nameSum] is None or not nameWight in arr[i] or arr[i][nameWight] is None:
      return None

  total = givenTotal
  if total is None:
    total = sumWithNone(arr,nameWight)

  if total is None:
    return None

  res = 0
  for i in range(len(arr)):
    res = res + arr[i][nameSum] * (arr[i][nameWight] / total)

  return res

def sumByWightOrOnlySum(arr,nameSum,nameWight,givenTotal=None):
  res = sumByWight(arr,nameSum,nameWight,givenTotal)
  if res is None:
    res = sumArrWithNone(arr,nameSum)
  return res

def sumWithNone(v1,v2):
  if v1 is None and v2 is None:
    return None
  res = 0
  if v1 is not None:
    res = res + v1
  if v2 is not None:
    res = res + v2
  return res

def subWithNone(v1,v2):
  if v1 is None and v2 is None:
    return None
  res = 0
  if v1 is not None:
    res = res + v1
  if v2 is not None:
    res = res - v2
  return res

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

  out_chargers = []
  for i in range(len(INIT_VICTRON_CHARGERS)):
    out_chargers.append(INIT_VICTRON_CHARGERS[i].read())

  out_inverters = []
  for i in range(len(INIT_VICTRON_INVERTERS)):
    out_inverters.append(INIT_VICTRON_INVERTERS[i].read())

  for i in range(len(INIT_HOYMILES)):
    hoyRes = INIT_HOYMILES[i].read()
    if hoyRes is None:
      continue
    for inv in hoyRes["inverters"]:
      out_inverters.append(inv)
    for char in hoyRes["chargers"]:
      out_chargers.append(char)

  out = {}

  out["inverters"] = out_inverters
  out["chargers"] = out_chargers

  #battery stuff
  out['batteryVoltage'] = sumArrWithNone(out_inverters+out_chargers,"batteryVoltage",True)

  #charger stuff
  out['chargeWatt'] = sumArrWithNone(out_chargers,'chargeWatt')
  out['chargeVoltage'] = sumByWightOrOnlySum(out_chargers,'chargeVoltage','chargeWatt',out['chargeWatt'])
  if out['chargeVoltage'] is not None and out['chargeWatt'] is not None:
    out['chargeAmpere'] = out['chargeWatt'] / out['chargeVoltage']

  if out['batteryVoltage'] is None:
    out['consumptionWatt'] = None
    out['consumptionVoltage'] = None
    out['consumptionAmpere'] = None
  else:
    out['consumptionWatt'] = sumArrWithNone(out_chargers,'consumptionWatt')
    out['consumptionVoltage'] = sumArrWithNone(out_chargers,'batteryVoltage',True)
    if out['consumptionWatt'] is not None and out['consumptionVoltage'] is not None:
      out['consumptionAmpere'] = out['consumptionWatt'] / out['consumptionVoltage']

  #inverter stuff
  out['consumptionInverterWatt'] = sumArrWithNone(out_inverters,'consumptionInverterWatt')
  out['consumptionInverterVoltage'] = sumArrWithNone(out_inverters,'consumptionInverterVoltage',True)
  if out['consumptionInverterWatt'] is not None and out['consumptionInverterVoltage'] is not None and out['consumptionInverterVoltage'] is not 0:
    out['consumptionInverterAmpere'] = out['consumptionInverterWatt'] / out['consumptionInverterVoltage']

  #final stuff
  out['totalConsumption'] = sumWithNone(out['consumptionInverterWatt'],out['consumptionWatt'])

  if out['batteryVoltage'] is None:
    out['batteryWatt'] = None
    out['batteryAmpere'] = None
  else:
    out['batteryWatt'] = subWithNone(out['chargeWatt'],out['totalConsumption'])
    out['batteryAmpere'] = out['batteryWatt'] / out['batteryVoltage']

  out['timestamp'] = round(time.time() * 1000)
  out['duration'] = POLL_TIME

  #this is just for my setup
  out['location'] = "home"
  out['device'] = "raspberrypi-camera-1"

  return out


resendMissingData()

stamp = datetime.now()

while running:
  out = readChargerAndInverter()

  if out is not None:
    jsonStr = json.dumps(out)
    print(jsonStr)
    #d.addEntry(jsonStr)
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