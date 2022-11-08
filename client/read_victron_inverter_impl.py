import traceback
from vedirect import Vedirect

INVERTER_POWER_FAC = 2400 / 3000
EFFICIENCY_FAC = 1.05
INVERTER_MODE_ECO = 5.
INVERTER_GROUND_CONSUMPTION = 20.

class VictronInverter:
  def __init__(self,port,name):

    self.name = name
    self.port = port
    self.connected = False

    self.charger = Vedirect(self.port)

  def read(self):
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

      output['name'] = self.name

      mode = int(ve_data["MODE"])
      output['consumptionInverterVoltage'] = int(ve_data["AC_OUT_V"]) / 100
      output['consumptionInverterAmpere'] = float(0)
      output['consumptionInverterWatt'] = float(0)
      if ve_data['Relay'] == "ON":
        if mode == INVERTER_MODE_ECO:
          output['consumptionInverterWatt'] = float(10)
        else:
          output['consumptionInverterWatt'] = INVERTER_GROUND_CONSUMPTION + int(ve_data["AC_OUT_S"]) * INVERTER_POWER_FAC * EFFICIENCY_FAC
          output['consumptionInverterAmpere'] = float(output['consumptionInverterWatt'] / output['consumptionInverterVoltage'])

        output['batteryVoltage'] = int(ve_data["V"]) / 1000
        output['batteryWatt'] = -output['consumptionInverterWatt']
        output['batteryAmpere'] = -output['consumptionInverterWatt']/output['consumptionInverterVoltage']
        output['temperature'] = None
      return output
    except Exception as ex:
      print("Caught exception while checking inverter")
      traceback.print_exc()
    return None
