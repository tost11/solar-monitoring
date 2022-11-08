#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Hoymiles micro-inverters main application
"""

import sys
from enum import IntEnum
import time
from datetime import datetime
import hoymiles

class InfoCommands(IntEnum):
  InverterDevInform_Simple = 0  # 0x00
  InverterDevInform_All = 1     # 0x01
  GridOnProFilePara = 2         # 0x02
  HardWareConfig = 3            # 0x03
  SimpleCalibrationPara = 4     # 0x04
  SystemConfigPara = 5          # 0x05
  RealTimeRunData_Debug = 11    # 0x0b
  RealTimeRunData_Reality = 12  # 0x0c
  RealTimeRunData_A_Phase = 13  # 0x0d
  RealTimeRunData_B_Phase = 14  # 0x0e
  RealTimeRunData_C_Phase = 15  # 0x0f
  AlarmData = 17                # 0x11, Alarm data - all unsent alarms
  AlarmUpdate = 18              # 0x12, Alarm data - all pending alarms
  RecordData = 19               # 0x13
  InternalData = 20             # 0x14
  GetLossRate = 21              # 0x15
  GetSelfCheckState = 30        # 0x1e
  InitDataState = 0xff

class HoymilesCharger:
  def __init__(self,serial,name):

    hoymiles.HOYMILES_DEBUG_LOGGING = True

    self.serial = serial
    self.name = name
    self.connected = False
    self.do_init = True

    hoymiles.HOYMILES_DEBUG_LOGGING = True

    self.nrf = {"ce_pin": 22,
           "cs_pin": 0,
           "txpower": 'low'}

    self.hmradio = hoymiles.HoymilesNRF(**self.nrf)

    self.command_queue = []
    self.event_message_index = 0

  def read(self, retries=4):
    """
    Send/Receive command_queue, initiate status poll on inverter

    :param str inverter: inverter serial
    :param retries: tx retry count if no inverter contact
    :type retries: int
    """

    # Queue at least status data request
    if self.do_init:
      self.command_queue.append(hoymiles.compose_send_time_payload(InfoCommands.InverterDevInform_All))
      self.do_init = False
    self.command_queue.append(hoymiles.compose_send_time_payload(InfoCommands.RealTimeRunData_Debug))

    # Put all queued commands for current inverter on air
    finalResult = None
    while len(self.command_queue) > 0:
      payload = self.command_queue.pop(0)

      # Send payload {ttl}-times until we get at least one reponse
      payload_ttl = retries
      while payload_ttl > 0:
        payload_ttl = payload_ttl - 1
        com = hoymiles.InverterTransaction(
          radio=self.hmradio,
          txpower='low',
          dtu_ser=self.serial,
          inverter_ser=self.serial,
          request=next(hoymiles.compose_esb_packet(
            payload,
            seq=b'\x80',
            src=self.serial,
            dst=self.serial
          )))
        response = None
        while com.rxtx():
          try:
            response = com.get_payload()
            payload_ttl = 0
          except Exception as e_all:
            print(f'Error while retrieving data: {e_all}')
            pass

      # Handle the response data if any

      if response:
        c_datetime = datetime.now()
        if hoymiles.HOYMILES_DEBUG_LOGGING:
          print(f'{c_datetime} Payload: ' + hoymiles.hexify_payload(response))
        decoder = hoymiles.ResponseDecoder(response,
                                           request=com.request,
                                           inverter_ser=self.serial
                                           )
        result = decoder.decode()
        if isinstance(result, hoymiles.decoders.StatusResponse):
          data = result.__dict__()

          #print(data)

          res = {}

          res["inverters"]=[]
          res["chargers"]=[]

          #print(f'{c_datetime} Decoded: temp={data["temperature"]}, total={data["energy_total"]/1000:.3f}', end='')

          if data['powerfactor'] is not None:
            res["powerfactor"]=data["powerfactor"]

          totalConsumption = 0
          phase_id = 0
          for phase in data['phases']:
            phase_id = phase_id + 1
            inverter = {
              'frequency':  data["frequency"],
              'consumptionInverterVoltage':phase["voltage"],
              'consumptionInverterWatt':phase["power"],
              'consumptionInverterAmpere':phase["current"],
              "name":self.name+" Phase " + str(phase_id),
              'temperature' : data["temperature"],
              'powerfactor' : data["powerfactor"]
            }
            totalConsumption = totalConsumption + phase["current"]
            res["inverters"].append(inverter)

          string_id = 0
          totalProduction = 0
          for string in data['strings']:
            string_id = string_id + 1

            charger = {
              'chargeVoltage':string["voltage"],
              'chargeWatt':string["power"],
              'chargeAmpere':string["current"],
              "name":self.name+" String " + str(string_id)
            }
            totalProduction = totalProduction + string["current"]
            res["chargers"].append(charger)

            #print(res)
            finalResult = res

          if 'event_count' in data:
            if self.event_message_index < data['event_count']:
              self.event_message_index = data['event_count']
              self.command_queue.append(hoymiles.compose_send_time_payload(InfoCommands.AlarmData, alarm_id=self.event_message_index))

    return finalResult

if __name__ == '__main__':

  h = HoymilesCharger(114182110459,"Carport Hoymiles")

  res = h.read()

  print(res)