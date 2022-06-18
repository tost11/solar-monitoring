import traceback
from vedirect import Vedirect

class VictronCharger:
  def __init__(self,port):

    self.port = port
    self.connected = False

    self.charger = Vedirect(self.port)

  def readCharger(self):
    try:
      try:
        ve_data = self.charger.read_data_single()
      except Exception as ex:
        print("first exception while reading charger")
        traceback.print_exc()
        self.charger = Vedirect(self.port)

      ve_data = self.charger.read_data_single()
      print("reading loader data")

      output ={}

      output['chargeVoltage'] = int(ve_data["VPV"]) / 1000
      output['chargeWatt'] = float(ve_data["PPV"])
      output['chargeAmpere'] = output['chargeWatt'] / output['chargeVoltage']

      output['batteryVoltage'] = int(ve_data["V"]) / 1000
      output['batteryAmpere'] = int(ve_data["I"]) / 1000
      output['batteryWatt'] = output['batteryAmpere'] * output['batteryVoltage']

      output['consumptionAmpere'] = int(ve_data["IL"]) / 1000
      output['consumptionWatt'] = output['consumptionAmpere'] * output['batteryVoltage']

      return output
    except Exception as ex:
      print("Caught exception while checking inverter")
      traceback.print_exc()
    return None