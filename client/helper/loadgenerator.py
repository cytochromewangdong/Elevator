"""
Copyright (c) 2018 VMware, Inc.  All rights reserved.
-- VMware Confidential
"""

import os
from os.path import join, dirname, isfile, exists

# This is used to load specified file, parse pre-defined
# request plan, and generate elevator workload.
class WorkLoadGenerator(object):

   def __init__(self, filePath=None, plan=None):
      if filePath:
         self.filePath = filePath
      else:
         self.filePath = join(dirname(__file__), 'dummyplan')
      self.plan = plan

      assert exists(self.filePath), ("%s doesn't exist" % self.filePath)
      assert exists(self.filePath), ("%s is not a file" % self.filePath)
      self.maxWorldClock = 0
      self._LoadPlan();

   # This internal function loads file with pre-defined user requests,
   # to fill in 2 tables:
   # 1. user request table, group by request timestamp;
   # 2. user stats table;
   def _LoadPlan(self):
      self.requests = {}
      self.userStats = {}
      if self.plan:
         lines = self.plan
      else:
         with open(self.filePath, mode='r') as f:
            lines = f.readlines()
      maxRequestTime = 0
      for line in lines:
         line = line.strip()
         if not line:
            # skip empty line
            continue
         if line[0] == '#':
            # skipp comment line:
            continue

         # request plan should be in format of:
         # ts, user, startFloor, destFloor
         ts, user, startFloor, destFloor = line.split(',')
         # convert everything to integer, which is easy to use
         ts = int(ts)
         assert ts > 0, "Time cannot be greater than 0"
         if ts > maxRequestTime:
            maxRequestTime = ts
         userIndex = int(user)
         sf = int(startFloor)
         df = int(destFloor)
         assert df != sf, "Bad user %d detected" % userIndex
         # Fill in the request per timestamp table
         requests = self.requests.get(ts, [])
         direction = 1 if df > sf else 0
         requests.append([userIndex, sf, direction])
         self.requests[ts] = requests
         # Initialize the user stats
         assert userIndex not in self.userStats, \
                "Duplicated user %d in request plan" % userIndex
         self.userStats[userIndex] = {
            'requestTime': ts,
            'startFloor': sf,
            'destFloor': df,
            'onBoard': False,
            'onBoardTime': 0,
            'arrived': False,
            'arrivedTime': 0,
            'elevator': -1,
         }
      self.maxWorldClock = maxRequestTime + 20 * len(self.userStats)

   # This function generates next workload based on specified
   # world timestamp and current elevator stats.
   def GenNext(self, ts, elevatorStats):
      return {
         'request': self.requests.get(ts, []),
         'goto': self._GenGotoRequests(ts, elevatorStats)
      }
      
   # This internal function generates 'goto' requests based on
   # specified elevator stats, and update latest user stats
   def _GenGotoRequests(self, ts, elevatorStats):
      assert isinstance(elevatorStats, dict) and len(elevatorStats) == 3, \
             "Incompleted elevator stats %s" % elevatorStats
      lastTs = ts - 1
      goto = []
      for elevator, stats in elevatorStats.iteritems():
         users = stats.get('users', [])
         assert len(users) == len(set(users)), \
                "User ID is duplicated."
         floor = stats['floor']
         for user in users:
            assert user in self.userStats, \
                   "User %d is not welcomed" % user
            # Detected that user is getting in an elevator
            if not self.userStats[user]['onBoard']:
               assert lastTs >= self.userStats[user]['requestTime'], \
                      "User %d entered elevator %d before he/she requests." % \
                      (user, elevator)
               assert floor == self.userStats[user]['startFloor'], \
                      "User requested elevator on %d floor, but got in on %d floor" % \
                      (self.userStats[user]['startFloor'], floor)
               self.userStats[user]['onBoard'] = True
               self.userStats[user]['onBoardTime'] = lastTs
               self.userStats[user]['elevator'] = elevator
               goto.append([
                  user,
                  self.userStats[user]['destFloor']
               ])
            else:
               assert self.userStats[user]['elevator'] == elevator, \
                      "User %d entered in elevator %d but show up in elevator %d." % \
                      (user, self.userStats[user]['elevator'], elevator)
               assert not self.userStats[user]['arrived'], \
                      "User %d has already arrived at %d, but still show up in elevator %d at %d" % \
                      (user, self.userStats[user]['arrivedTime'], elevator, lastTs)
               assert (self.userStats[user]['onBoardTime'] - lastTs) < 40, \
                      "User %d has been in elevator %d for 40 time units!!!" % (user, elevator)
         for user, userStats in self.userStats.iteritems():
            if userStats['onBoard'] and \
               userStats['elevator'] == elevator and \
               not userStats['arrived'] and \
               user not in users:
               # Detect user getting off from elevator
               assert floor == userStats['destFloor'], \
                      "User %d to %d floor has been kicked out at %d by elevator %d" % \
                      (user, userStats['destFloor'], floor, elevator)
               userStats['arrivedTime'] = lastTs
               userStats['arrived'] = True
      return goto

   # This function goes through all user stats, to figure out
   # whether all users have arrived
   def IsDone(self):
      for user, userStats in self.userStats.iteritems():
         if not userStats['arrived']:
            return False
      return True

   # Based on current user stats, to generate summary report, which includes:
   # total expected power cost,
   # average customer satisfication
   def GetSummary(self):
      expectedPowerCost = 0
      satisfactionRate = 0
      for user, userStats in self.userStats.iteritems():
         moves = abs(userStats['destFloor'] - userStats['startFloor'])
         if userStats['destFloor'] > userStats['startFloor']:
            # going up
            expectedPowerCost += moves * 0.6
         else:
            # going down
            expectedPowerCost += moves * 0.4
         if userStats['arrived']:
            expectedTime = moves
            actualTime = userStats['arrivedTime'] - userStats['requestTime']
            satisfactionRate += (100 - 0.05 * (actualTime + 1) * actualTime)/100
      return expectedPowerCost, satisfactionRate/len(self.userStats)
