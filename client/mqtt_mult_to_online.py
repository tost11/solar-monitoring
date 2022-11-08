import paho.mqtt.client as mqtt
import json
import traceback
import requests

API_ENDPOINT = "https://solar.pihost.org/api/solar/data/selfmade/consumption?systemId=10"
TOKEN = "0628732f-7e1f-43e7-a237-d9ea9d60176a"

def on_connect(client, userdata, flags, rc):
    print("Connected with result code "+str(rc))
    client.subscribe("mult-charger-event")

def on_message(client, userdata, msg):
    print(msg.payload.decode())
    try:
        input = json.loads(msg.payload.decode())
    except Exception as ex:
        print("Invalid json skipping it")
        print(str(ex))
        return

    data = {'timestamp': input['timestamp'],
            'duration': input['duration'],
            'chargeVoltage': None,
            'chargeAmpere': None,
            'chargeWatt': None,
            'batteryVoltage': input['batteryVoltage'],
            'batteryAmpere': input['batteryAmpere'],
            'batteryWatt': input['batteryWatt'],
            'consumptionVoltage': input['batteryVoltage'],
            'consumptionAmpere': input['totalConsumption'] / input['batteryVoltage'],
            'consumptionWatt': input['totalConsumption'],
            'totalConsumption': input['totalConsumption']
            }

    pannelVoltage = 0
    i = 0
    for device_input in input['inputs']:
        i = i+1
        pannelVoltage += device_input["chargeVoltage"]

    if i > 0:
        pannelVoltage = pannelVoltage / i
        data["chargeWatt"] = input["totalProduction"]
        data["chargeAmpere"] = input["totalProduction"] / pannelVoltage
        data["chargeVoltage"] = input["totalProduction"] = pannelVoltage

    print(data)

    headers = {'clientToken': TOKEN}
    try:
        r = requests.post(url=API_ENDPOINT, headers=headers, json=data)
        print(r)

        if r.status_code != 200:
            print("Error in sending request")
            print(r.content)
    except Exception as ex:
        print("Execption while sending data to backend")
        print(traceback.format_exc())

client = mqtt.Client()
client.username_pw_set("pi","password")
client.connect("localhost",1883,60)

client.on_connect = on_connect
client.on_message = on_message

client.loop_forever()