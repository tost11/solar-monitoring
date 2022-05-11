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

    threading.Timer(60, self.reconnectIfNeeded).start()

  def connect(self):
    print("connection to Kafka")
    try:
      self.producer = KafkaProducer(bootstrap_servers=self.hosts, client_id=self.client)
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
      self.producer.send(topic, key=ENCODING, value=bytes(message, 'utf-8'))
      self.producer.flush()
    except Exception as ex:
      print("Caught exception while sending message to kafka")
      traceback.print_exc()
      return False
    return True