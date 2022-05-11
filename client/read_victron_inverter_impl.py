import traceback
from vedirect import Vedirect

INVERTER_POWER_FAC = 2400 / 3000
EFFICIENCY_FAC = 1.05
INVERTER_MODE_ECO = 5.
INVERTER_GROUND_CONSUMPTION = 20.

def readInverter(EPEVER_INVERTER_PORT):
  try:
    ve = Vedirect(EPEVER_INVERTER_PORT)
    ve_data = ve.read_data_single()
    print("reading inverter data")

    output ={}

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

      output['batteryVoltage'] = int(ve_data["V"]) / 100
      output['batteryWatt'] = -output['consumptionInverterWatt']
      output['batteryAmpere'] = -output['consumptionInverterWatt']/output['consumptionInverterVoltage']
      output['totalConsumption'] = output['consumptionInverterWatt']
      output['inverterTemperature'] = None
      return output
  except Exception as ex:
    print("Caught exception while checking inverter")
    traceback.print_exc()
  return None