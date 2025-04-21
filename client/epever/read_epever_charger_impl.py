import traceback
from pymodbus.client.sync import ModbusSerialClient as ModbusClient

def get_loader_client(loader_port):
  return ModbusClient(
    method="rtu",
    port=loader_port,
    baudrate=115200,
    timeout=1
  )

def readCharger(EPEVER_LOADER_PORT):

  print("\n\nCheck for data")
  output = {}
  output['total_consumption'] = 0

  print("Checking Loader")

  try:
    client = get_loader_client(EPEVER_LOADER_PORT)
    if client.connect():
      result = client.read_input_registers(0x3100, 19, unit=1)
      if isinstance(result, Exception):
        print("Could not read data from Loader Adresses 0x3100 - 0x3118")
      else:
        if result.function_code < 0x80:

          output['chargeVoltage'] = result.registers[0]/100
          output['chargeAmpere'] = result.registers[1]/100
          output['chargeWatt'] = output['chargeVoltage'] * output['chargeAmpere']

          output['consumptionVoltage'] = result.registers[12]/100
          output['consumptionAmpere'] = result.registers[13]/100
          output['consumptionWatt'] = output['consumptionVoltage'] * \
                                     output['consumptionAmpere']

          output['batteryVoltage'] = result.registers[4]/100
          output['batteryAmpere'] = result.registers[5] / \
                                     100 - output['consumptionAmpere']
          output['batteryWatt'] = output['batteryVoltage'] * \
                                   output['batteryAmpere']

          output['deviceTemperature'] = result.registers[17]/100
          output['batteryTemperature'] = result.registers[16]/100

          output['totalConsumption'] = output['consumptionWatt']
          return output
        else:
          print("Bad checksum on loader result")
    else:
      print("Connection to loader not possible")
  except Exception as ex:
    print("Caught exception while checking loader")
    traceback.print_exc()
  return None
