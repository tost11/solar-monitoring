
from read_victron_charger_impl import VictronCharger
from local_data_storage import MyDatabase
from datetime import datetime
import time
from send_to_mqtt import Producer
import json
import signal
import os
import time, threading

loadersToRead = []
loadersToRead.append(VictronCharger("/dev/ttyUSB0"))

POLL_TIME = 5.
TOPIC = "mult-charger-event"

running = True

k = Producer(["localhost"])
d = MyDatabase("data_2.db")

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
  if k.connected is True:
    enties = d.getEntries(100,0)
    print("Try sending",len(enties),"missing Data")
    for e in enties:
      res = k.sendMessage(TOPIC,e.data)
      if res is False:
        print("Stopped sending missing entries because of connection loss")
        return
      d.removeEntry(e.id)
      print("Succesfull resend entry ",e.id)
      print(e.data)

def readChargerAndInverter():

  i = 0
  totalConsumption = 0
  totalProduction = 0
  out = {}
  out['inputs'] = []
  batteryVoltage = None
  batteryAmpere = 0
  batteryWatt = 0

  for charger in loadersToRead:
    i = i + 1
    out_curr = charger.readCharger()

    if out_curr is not None:
      if batteryVoltage is None:
        batteryVoltage = out_curr['batteryVoltage']
      else:
        batteryVoltage = (batteryVoltage * (1 - 1/i)) + (out_curr['batteryVoltage'] *  1 / i )
      batteryAmpere += out_curr["batteryAmpere"]
      batteryWatt += out_curr["batteryWatt"]
      out_curr["id"] = i
      out_curr.pop("batteryWatt")
      out_curr.pop("batteryAmpere")
      out_curr.pop("batteryVoltage")
      totalConsumption += out_curr['consumptionWatt']
      totalProduction += out_curr['chargeWatt']
      out['inputs'].append(out_curr)

  if batteryVoltage is None:
    return None

  out['totalConsumption'] = float(totalConsumption)
  out['totalProduction'] = float(totalProduction)
  out['batteryVoltage'] = float(batteryVoltage)
  out['batteryAmpere'] = float(batteryAmpere)
  out['batteryWatt'] = float(batteryWatt)

  out['timestamp'] = round(time.time() * 1000)
  out['duration'] = float(POLL_TIME)

  return out

resendMissingData()
threading.Timer(60*5, resendMissingData).start()

stamp = datetime.now()

while running:
  out = readChargerAndInverter()

  if out is not None:
    jsonStr = json.dumps(out)
    res = k.sendMessage(TOPIC,jsonStr)
    if res is False:
      d.addEntry(jsonStr)
    else:
      print(jsonStr)

  now = datetime.now()
  dif = now - stamp

  timeToSleep = POLL_TIME - dif.total_seconds()
  if(timeToSleep > 0):
    print("Sleep for: ", timeToSleep, " Seconds")
    time.sleep(timeToSleep)

  stamp = datetime.now()

os._exit(0)
