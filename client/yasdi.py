from subprocess import Popen, PIPE
import time
import traceback
import requests
from datetime import datetime

p = Popen(['yasdishell', 'yasdi.ini'],stdin = PIPE, stdout=PIPE, stderr=PIPE,bufsize=0,close_fds=True)
xs = bytearray(b'')

POLL_TIME = 30
API_ENDPOINT = "https://{YOUR URL HERE}/api/solar/data/grid/devices?systemId={YOUR SYSTEM ID HERE}"
HEADERS = {'clientToken':"{YOUR TOKEN HERE}"}

C_FINDDEVICES="e\n4\n"
#insert your device ids here
C_READ_1="a\n155\n"
C_READ_2="a\n385\n"
C_READ_3="a\n545\n"

def addToDTO(dto,target,value,inputId=None):
  if dto == None:
    dto = {"inputs":[]}

  if inputId == None:
    dto[target] = float(value)
    return dto

  input = None
  for inp in dto["inputs"]:
    if inp["id"] == inputId:
      input = inp
      break

  if input == None:
    input = {"id":inputId}
    dto["inputs"].append(input)

  input[target] = value
  return dto

def parseLine(line):
  try:
    arr = line.split("|")
    if len(arr) != 3:
      return None
    res = [None] * 2
    res[0] = arr[1].split("'")[1].strip()
    res[1] = float(arr[2].split("'")[1].strip())
    return res
  except:
    return None

def handleFinish(cC,commands):
  global devices
  global ok
  if cC == C_FINDDEVICES:
    if "Sorry, no devices currently available..." in cC:
      ok = False
    else:
      ok = True
    #TODO late parse devices maby
    return

  dto = None
  print(dto)
  #example of parsing for charger with two configured inputs
  if cC == C_READ_1:
    for line in commands:
      arr = parseLine(line)
      if arr == None:
        continue
      if arr[0] == "Upv-Ist DC-A":
        dto = addToDTO(dto,"voltage",arr[1],1)
      if arr[0] == "Upv-Ist DC-B":
        dto = addToDTO(dto,"voltage",arr[1],2)
      if arr[0] == "PPV DC-A":
        dto = addToDTO(dto,"watt",arr[1],1)
      if arr[0] == "PPV DC-B":
        dto = addToDTO(dto,"watt",arr[1],2)
      if arr[0] == "Uac":
        dto = addToDTO(dto,"gridVoltage",arr[1])
      if arr[0] == "Pac":
        dto = addToDTO(dto,"gridWatt",arr[1])
      if arr[0] == "Fac":
        dto = addToDTO(dto,"frequency",arr[1])
      if arr[0] == "E-Total":
        dto = addToDTO(dto,"totalKWH",arr[1])
      if arr[0] == "h-Total":
        dto = addToDTO(dto,"totalOH",arr[1])
    if dto != None:
      dto["id"]=155
      #until backend is fixed
      if dto["inputs"][0]["voltage"] <= 0:
        dto["inputs"][0]["ampere"] = 0
      else:
        dto["inputs"][0]["ampere"] = dto["inputs"][0]["watt"] / dto["inputs"][0]["voltage"]
      #utnil backend is fixed
      if dto["inputs"][1]["voltage"] <= 0:
        dto["inputs"][1]["ampere"] = 0
      else:
        dto["inputs"][1]["ampere"] = dto["inputs"][1]["watt"] / dto["inputs"][1]["voltage"]
      #until backend is fixed
      if dto["gridVoltage"] <= 0:
        dto["gridAmpere"] = 0
      else:
        dto["gridAmpere"] = dto["gridWatt"] / dto["gridVoltage"]
      devices.append(dto)
    print("dto is",dto)

  #example of parsing for charger with two configured inputs
  elif cC == C_READ_2:
    for line in commands:
      arr = parseLine(line)
      if arr == None:
        continue
      if arr[0] == "Upv-Ist":
        dto = addToDTO(dto,"chargeVoltage",arr[1])
      if arr[0] == "Ipv":
        dto = addToDTO(dto,"chargeAmpere",arr[1]/1000)
      if arr[0] == "Uac":
        dto = addToDTO(dto,"gridVoltage",arr[1])
      if arr[0] == "Pac":
        dto = addToDTO(dto,"gridWatt",arr[1])
      if arr[0] == "Fac":
        dto = addToDTO(dto,"frequency",arr[1])
      if arr[0] == "E-Total":
        dto = addToDTO(dto,"totalKWH",arr[1])
      if arr[0] == "h-Total":
        dto = addToDTO(dto,"totalOH",arr[1])
    if dto != None:
      dto["id"]=385
      #until backend is fixed
      if dto["gridVoltage"] <= 0:
        dto["gridAmpere"] = 0
      else:
        dto["gridAmpere"] = dto["gridWatt"] / dto["gridVoltage"]
      devices.append(dto)
    print("dto is",dto)

  elif cC == C_READ_3:
    for line in commands:
      arr = parseLine(line)
      if arr == None:
        continue
      if arr[0] == "Upv-Ist DC-A":
        dto = addToDTO(dto,"voltage",arr[1],3)
      if arr[0] == "Upv-Ist DC-B":
        dto = addToDTO(dto,"voltage",arr[1],4)
      if arr[0] == "PPV DC-A":
        dto = addToDTO(dto,"watt",arr[1],3)
      if arr[0] == "PPV DC-B":
        dto = addToDTO(dto,"watt",arr[1],4)
      if arr[0] == "Uac":
        dto = addToDTO(dto,"gridVoltage",arr[1])
      if arr[0] == "Pac":
        dto = addToDTO(dto,"gridWatt",arr[1])
      if arr[0] == "Fac":
        dto = addToDTO(dto,"frequency",arr[1])
      if arr[0] == "E-Total":
        dto = addToDTO(dto,"totalKWH",arr[1])
      if arr[0] == "h-Total":
        dto = addToDTO(dto,"totalOH",arr[1])
    if dto != None:
      dto["id"]=454
      #until backend is fixed
      if dto["inputs"][0]["voltage"] <= 0:
        dto["inputs"][0]["ampere"] = 0
      else:
        dto["inputs"][0]["ampere"] = dto["inputs"][0]["watt"] / dto["inputs"][0]["voltage"]
      #utnil backend is fixed
      if dto["inputs"][1]["voltage"] <= 0:
        dto["inputs"][1]["ampere"] = 0
      else:
        dto["inputs"][1]["ampere"] = dto["inputs"][1]["watt"] / dto["inputs"][1]["voltage"]
      #until backend is fixed
      if dto["gridVoltage"] <= 0:
        dto["gridAmpere"] = 0
      else:
        dto["gridAmpere"] = dto["gridWatt"] / dto["gridVoltage"]
      devices.append(dto)
    print("dto is",dto)

def current_milli_time():
  return round(time.time() * 1000)

def sendData(devices):
  global POLL_TIME
  global API_ENDPOINT
  global HEADERS

  data = {
    "duration": POLL_TIME,
    "timestamp": current_milli_time(),
    "devices": devices
  }

  try:
    r = requests.post(url = API_ENDPOINT,headers = HEADERS, json = data)
    print(r)

    if r.status_code != 200:
      print(r.content)
  except:
    print('requests fail')
    print(data)
    print(traceback.format_exc())

nextCommand=C_READ_1
currentCommand=""
summedLines = list()
stamp = datetime.now()
first = True
devices = list()
ok = False

while True:

  #c=reader.read(1)
  c = p.stdout.read(1)
  xs += c

  decoded = xs.decode()

  if decoded.endswith('\n'):

    summedLines.append(decoded)
    print("-> ",decoded)

    xs = bytearray(b'')
    continue

  if decoded == "Command ('?' for help): ":
    print("Processing mext Command")
    handleFinish(currentCommand,summedLines)
    summedLines.clear()

    #change last command when request should be send
    if currentCommand == C_READ_3:
      print("send devices")
      print(devices)

      if len(devices) == 0:
        print("No data available on any Device so weit for 5 min and try again")
        time.sleep(360)
      else:
        sendData(devices)

      devices.clear()

      now = datetime.now()
      dif = now - stamp
      stamp = now
      timeToSleep = POLL_TIME - dif.total_seconds()
      if(timeToSleep > 0):
        print("Sleep for: ", timeToSleep, " Seconds")
        time.sleep(timeToSleep)


    #for testing
    #ok = True

    if ok == False:
      if first == False:
        print("No Devices available wait for 5 min and try again")
        time.sleep(360)
        first = False
      #p.stdin.write(bytes(C_FINDDEVICES,'UTF-8'))
      #p.stdin.flush()
      nextCommand = C_FINDDEVICES

    print("--> send command",nextCommand)
    p.stdin.write(bytes(nextCommand,'UTF-8'))
    p.stdin.flush()

    currentCommand=nextCommand

    #change commandorder if changes
    if nextCommand == C_FINDDEVICES:
      nextCommand=C_READ_1
    elif nextCommand == C_READ_1:
      nextCommand=C_READ_2
    elif nextCommand == C_READ_2:
      nextCommand=C_READ_3
    elif nextCommand == C_READ_3:
      nextCommand=C_READ_1