import paho.mqtt.client as mqtt
import json
from influxdb import InfluxDBClient
import time

# This is the Subscriber
influxClient = InfluxDBClient(host='localhost', port=8086,username='grafana', password='password')
influxClient.switch_database('home')

def on_connect(client, userdata, flags, rc):
    print("Connected with result code "+str(rc))
    client.subscribe("mult-charger-event")

def on_message(client, userdata, msg):
    try:
        input = json.loads(msg.payload.decode())
    except Exception as ex:
        print("Invalid json skipping it")
        print(str(ex))
        return

    json_body = [
        {
            "measurement": "solar",
            "time": input['timestamp'],
            "fields": {
                "batteryVoltage": input['batteryVoltage'],
                "batteryAmpere": input['batteryAmpere'],
                "batteryWatt": input['batteryWatt'],
                "totalConsumption": input['totalConsumption'],
                "totalProduction": input['totalProduction'],
                "duration": input['duration']
            }
        }
    ]

    for device_input in input['inputs']:
        json_device = {
            "measurement": "solar-input",
            "tags": {
                "id": device_input['id'],
            },
            "time": input['timestamp'],
            "fields": {
                "chargeVoltage": device_input['chargeVoltage'],
                "chargeWatt": device_input['chargeWatt'],
                "chargeAmpere": device_input['chargeAmpere'],
                "consumptionVoltage": input['batteryVoltage'],
                "consumptionWatt": device_input['consumptionWatt'],
                "consumptionAmpere": device_input['consumptionAmpere']
            }
        }

        json_body.append(json_device)

    print(json_body)

    ok=False
    while not ok:
        try:
            influxClient.write_points(json_body,time_precision='ms')
            ok=True
        except Exception as ex:
            print("Could not write to influx")
            print(str(ex))
            time.sleep(10)


client = mqtt.Client()
client.username_pw_set("pi","password")
client.connect("localhost",1883,60)

client.on_connect = on_connect
client.on_message = on_message

client.loop_forever()