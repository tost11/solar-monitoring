import traceback
import time, threading
from kafka import KafkaProducer

ENCODING = bytes('raw', encoding='utf-8')

class MyKafkaProducer:
  def __init__(self,hosts, client):

    self.hosts = hosts
    self.client = client
    self.connected = False

    self.connect()

    #threading.Timer(60, self.reconnectIfNeeded).start()

  def connect(self):
    print("connection to Kafka")
    try:
      self.producer = KafkaProducer(bootstrap_servers=self.hosts, client_id=self.client)
      self.connected = True
    except Exception as ex:
      print("Caught exception while connecting kafka producer try again in 60s")
      traceback.print_exc()
      threading.Timer(60, self.connect).start()

  def flush(self):
    try:
      self.producer.flush()
    except Exception as ex:
      print("Caught exception while flushing kafka producer")
      traceback.print_exc()

  def disconnect(self):
    print("disconnection to Kafka")
    try:
      self.producer.close()
    except Exception as ex:
      print("Caught exception while disconnecting kafka producer")
      #traceback.print_exc()
    #self.connected = False

  #def reconnectIfNeeded(self):
  #  print("check for reconnect")
  #  if self.connected is False:
  #    self.disconnect()
  #    self.connect()
  #  threading.Timer(60, self.reconnectIfNeeded).start()

  def sendMessage(self,topic,message):
    #if self.connected is False:
    #  print("Could not send message because producer is not connected")
    #  return None
    try:
      return self.producer.send(topic, key=ENCODING, value=bytes(message, 'utf-8'))
    except Exception as ex:
      print("Caught exception while sending message to kafka -> set kafaka producer to disconnecdted")
      traceback.print_exc()
      #self.connected = False
      return None