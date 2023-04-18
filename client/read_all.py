#!/usr/local/bin/python3 -u

#TODO check if last code changes are working

from read_victron_inverter_threaded_impl import VictronInverterThreaded
from read_victron_charger_threaded_impl import VictronChargerThreaded
from read_hoymiles import HoymilesCharger
from local_data_storage import MyDatabase
from datetime import datetime
import json
import signal
import os
import time, threading
import requests

#TODO change for your usaage
POLL_TIME = 5.
INIT_VICTRON_CHARGERS = [VictronChargerThreaded("/dev/ttyUSB0","Loader 1",POLL_TIME/2),VictronChargerThreaded("/dev/ttyUSB2","Loader2",POLL_TIME/2),VictronChargerThreaded("/dev/ttyUSB3","Loader 3",POLL_TIME/2)]
INIT_VICTRON_INVERTERS = [VictronInverterThreaded("/dev/ttyUSB1","Inverter 1",POLL_TIME/2)]
INIT_HOYMILES = [HoymilesCharger(123456789123,"Hoymiles")]
API_ENDPOINT = "https://solar.pihost.org/api/solar/data/mult?systemId=YOUR_ID_HERE"
TOKEN="YOUR_API_TOKEN_HERE"
#end of change section

running = True
resendFinished = False

d = MyDatabase("data.db")

def signal_handler(sig, frame):
  global running
  print('You pressed Ctrl+C -> program will try to terminate gently')
  if running is False:
    print('hard termination because second try')
    os._exit(1)
  running = False

signal.signal(signal.SIGINT, signal_handler)

def sumArrWithNone(arr,name,device=False):
  ok = 0
  res = None
  for obj in arr:
    if obj is None:
      continue
    if not name in obj:
      continue
    if obj[name] is None:
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
  if len(arr) == 0:
    return None

  for i in range(len(arr)):
    #print(arr[i])
    if arr[i] is None or arr[i][nameSum] is None or arr[i][nameWight] is None:
      return None

  total = givenTotal
  if total is None:
    total = sumWithNone(arr,nameWight)

  if total is None or total == 0:
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
  #print("Resend missing data")
  global running
  global resendFinished
  NUM = 10
  while running:
    enties = d.getEntries(NUM,0)
    if len(enties) == 0:
      break
    #print("Try sending",len(enties),"missing Data")
    i = 0
    data = []
    for e in enties:
      data.append(e.data)

    headers = {'clientToken':TOKEN}
    try:
      r = requests.post(url = API_ENDPOINT,headers = headers, json = data)
      print(r)
      print(r.content)
      if r.status_code == 200:
        print("Data succesfull send")
      elif r.status_code == 401:
        print("Token not exist (401)")
        os._exit(1)
      elif r.status_code == 400:
        print("Something in request is wrong (400)")
        os._exit(1)
      else:
        print("Error on sending data with error ({})",r.status_code)

      for e in enties:
        d.removeEntry(e.id)
        print("Succesfull resend entry ",e.id)
    except:
      print('requests fail')
      print(traceback.format_exc())

  if running:
    threading.Timer(5, resendMissingData).start()
  else:
    resendFinished = True

def readChargerAndInverter():

  out_chargers = [None] * (len(INIT_VICTRON_CHARGERS) + len(INIT_HOYMILES)*2)
  for i in range(len(INIT_VICTRON_CHARGERS)):
    out_chargers[i] = INIT_VICTRON_CHARGERS[i].read()

  out_inverters = [None] * (len(INIT_VICTRON_INVERTERS) + len(INIT_HOYMILES))
  for i in range(len(INIT_VICTRON_INVERTERS)):
    out_inverters[i] = INIT_VICTRON_INVERTERS[i].read()

  for i in range(len(INIT_HOYMILES)):
    hoyRes = INIT_HOYMILES[i].read()
    if hoyRes is None:
      continue
    for j in len(hoyRes["inverters"]):
      out_inverters[len(INIT_VICTRON_INVERTERS)+i+j] = hoyRes["inverters"]
    for j in len(hoyRes["chargers"]):
      out_chargers[len(INIT_VICTRON_CHARGERS)+i*2+j] = hoyRes["chargers"]

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

  out['consumptionWatt'] = sumArrWithNone(out_chargers,'consumptionWatt')
  out['consumptionVoltage'] = sumArrWithNone(out_chargers,'batteryVoltage',True)
  if out['consumptionWatt'] is not None and out['consumptionVoltage'] is not None:
    out['consumptionAmpere'] = out['consumptionWatt'] / out['consumptionVoltage']

  #inverter stuff
  out['consumptionInverterWatt'] = sumArrWithNone(out_inverters,'consumptionInverterWatt')
  out['selfConsumptionInverterWatt'] = sumArrWithNone(out_inverters,'selfConsumptionInverterWatt')
  out['consumptionInverterVoltage'] = sumArrWithNone(out_inverters,'consumptionInverterVoltage',True)
  if out['consumptionInverterWatt'] is not None and out['consumptionInverterVoltage'] is not None and out['consumptionInverterVoltage'] != 0:
    out['consumptionInverterAmpere'] = out['consumptionInverterWatt'] / out['consumptionInverterVoltage']

  #final stuff
  out['totalConsumption'] = sumWithNone(out['consumptionInverterWatt'],out['consumptionWatt'])
  out['batteryWatt'] = subWithNone(out['chargeWatt'],out['totalConsumption'])
  out['batteryAmpere'] = out['batteryWatt'] / out['batteryVoltage']

  out['timestamp'] = round(time.time() * 1000)
  out['duration'] = POLL_TIME

  return out


resendMissingData()

stamp = datetime.now()

while running:
  out = readChargerAndInverter()

  if out is not None:
    jsonStr = json.dumps(out)
    #print(jsonStr)
    d.addEntry(jsonStr)
  else:
    print("out is zero so no data can be send")

  now = datetime.now()
  dif = now - stamp

  timeToSleep = POLL_TIME - dif.total_seconds()
  #print("sleeptime is: ",timeToSleep)
  if(timeToSleep > 0):
    #print("Sleep for: ", timeToSleep, " Seconds")
    time.sleep(timeToSleep)
  stamp = datetime.now()

print("stop all chargers")
for i in range(len(INIT_VICTRON_CHARGERS)):
  INIT_VICTRON_CHARGERS[i].stop()

print("stop all inverters")
for i in range(len(INIT_VICTRON_INVERTERS)):
  INIT_VICTRON_INVERTERS[i].stop()

print("wait for chargers to terminate")
for i in range(len(INIT_VICTRON_CHARGERS)):
  v = INIT_VICTRON_CHARGERS[i]
  del v

print("wait for inverters to terminate")
for i in range(len(INIT_VICTRON_INVERTERS)):
  v = INIT_VICTRON_INVERTERS[i]
  del v

print("wait for resend to be finished")
while resendFinished is False:
  time.sleep(1)

os._exit(0)