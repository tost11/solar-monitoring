import traceback
from read_victron_inverter_impl import VictronInverter
from threading import Thread
from datetime import datetime
import time
from multiprocessing import Process, Lock

class VictronInverterThreaded:
  def __init__(self,port,name,timeout):
    self.charger = VictronInverter(port,name)
    self.lastResult = None
    self.running = True
    self.timeout = timeout
    self.mutex = Lock()

    self.tread = Thread(target=self.run_read, args=())
    self.tread.start()

  def __del__(self):
    self.stop()
    self.tread.join()

  def stop(self):
    self.running = False

  def read(self):
    with self.mutex:
      data = self.lastResult
      self.lastResult = None
      return data

  def run_read(self):

    stamp = datetime.now()

    while self.running:

      data = self.charger.read()

      while data is None:

        print("None data on",self.charger.name)

        data = self.charger.read()

        time.sleep(self.timeout / 10)

        now = datetime.now()
        dif = now - stamp
        timeToSleep = self.timeout - dif.total_seconds()
        if(timeToSleep <= 0):
          print("Read timeout on ",self.charger.name)
          break

      with self.mutex:
        self.lastResult = data

      now = datetime.now()
      dif = now - stamp

      timeToSleep = self.timeout - dif.total_seconds()
      #print("sleeptime is: ",timeToSleep)
      if(timeToSleep > 0):
        print("Sleep for: ", timeToSleep, "Seconds for",self.charger.name)
        time.sleep(timeToSleep)
      stamp = datetime.now()