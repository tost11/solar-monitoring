import sqlite3
import threading

class SolarDbEntry:
  def __init__(self, id, data, retrys):
    self.id = id
    self.data  = data
    self.retrys  = retrys

class MyDatabase:
  def __init__(self, filename):
    self.lock = threading.Lock()
    self.database = sqlite3.connect(filename,check_same_thread=False)
    c = self.database.cursor()
    sql = 'CREATE TABLE if not exists solardata (id INTEGER PRIMARY KEY AUTOINCREMENT, data TEXT NOT NULL , retrys INTEGER NOT NULL)'
    c.execute(sql)
    self.database.commit()


  def addEntry(self,entry):
    with self.lock:
      print("Added new entry to database")
      c = self.database.cursor()

      sql = 'INSERT INTO solardata (data,retrys) values (?,?)'
      c.execute(sql,(entry,0))
      self.database.commit()

  def getEntries(self,size,offset):
    res = []
    c = self.database.cursor()

    sql = 'SELECT * from solardata limit ? offset ?'
    rows = c.execute(sql,(size,offset)).fetchall()
    for row in rows:
      res.append(SolarDbEntry(*row))
    return res

  def getEntriesCheckRetrys(self,retrys,size,offset):
    res = []
    c = self.database.cursor()

    sql = 'SELECT * from solardata where retrys < ? limit ? offset ?'
    rows = c.execute(sql,(retrys,size,offset)).fetchall()
    for row in rows:
      res.append(SolarDbEntry(*row))
    return res

  def increaseRetries(self,numbers):
    with self.lock:
      if len(numbers) <= 0:
        return
      c = self.database.cursor()
      sql = 'UPDATE solardata SET retrys = retrys + 1 where id IN (' + (",".join(map(str, numbers))) + ')'
      c.execute(sql)
      self.database.commit()

  def removeEntry(self,id):
    with self.lock:
      c = self.database.cursor()
      sql = 'DELETE FROM solardata where id == '+str(id)
      c.execute(sql)
      self.database.commit()
