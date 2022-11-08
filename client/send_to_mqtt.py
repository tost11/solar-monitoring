import traceback
import time, threading
import paho.mqtt.client as mqtt

class Producer:
  def __init__(self,hosts):

    self.hosts = hosts
    self.connected = False

    self.producer = mqtt.Client()
    self.producer.username_pw_set("pi","password")

    self.connect()

    threading.Timer(60, self.reconnectIfNeeded).start()

  def connect(self):
    print("connection to Mqtt")
    try:
      self.producer.connect("localhost",1883,60)
      self.connected = True
    except Exception as ex:
      print("Caught exception while connecting kafka producer")
      traceback.print_exc()

  def reconnectIfNeeded(self):
    print("check for reconnect")
    if self.connected is False:
      self.connect()

  def sendMessage(self,topic,message):
    try:
      self.producer.publish(topic, message)
    except Exception as ex:
      print("Caught exception while sending message to kafka")
      traceback.print_exc()
      return False
    return True