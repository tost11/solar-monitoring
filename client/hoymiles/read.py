#!/usr/local/bin/python3 -u

#TODO check if last code changes are working

from datetime import datetime
import json
import signal
import os
import time, threading
import requests

#TODO change for your usaage
POLL_TIME = 5.
INIT_HOYMILES = HoymilesCharger(123456789123,"Hoymiles")
API_ENDPOINT = "https://solar.pihost.org/api/solar/data/mult?systemId=YOUR_ID_HERE"
TOKEN="YOUR_API_TOKEN_HERE"
#end of change section

running = True
resendFinished = False

def signal_handler(sig, frame):
  global running
  print('You pressed Ctrl+C -> program will try to terminate gently')
  if running is False:
    print('hard termination because second try')
    os._exit(1)
  running = False

signal.signal(signal.SIGINT, signal_handler)


def sendData(data):
  print("Sending data:",data)
  headers = {'clientToken':TOKEN}
  try:
    r = requests.post(url = API_ENDPOINT,headers = headers, json = data)
    if r.status_code == 200:
      print("Data succesfull send")
    else:
      print("Error on sending data with error ({})",r.status_code)
    except:
      print('requests fail')
      print(traceback.format_exc())


stamp = datetime.now()

while running:

  out = INIT_VICTRON_CHARGERS.read()

  if out is not None:
     out['timestamp'] = round(time.time() * 1000)
    out['duration'] = POLL_TIME

    jsonStr = json.dumps(out)
    sendData(out)
  else:
    print("no data collected so no data can be send")

  now = datetime.now()
  dif = now - stamp

  timeToSleep = POLL_TIME - dif.total_seconds()
  if(timeToSleep > 0):
    time.sleep(timeToSleep)
  stamp = datetime.now()

os._exit(0)