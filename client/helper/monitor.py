"""
Copyright (c) 2018 VMware, Inc.  All rights reserved.
-- VMware Confidential
"""

# This class helps to pase the elevator stats retrieved from server side,
# and maintain status table for all 3 elevators at client side.
# It can also help to find out any foul elevator.
class ElevatorMonitor(object):

   def __init__(self, initStats):
      self.idleCount = 0
      self.initialized = False
      self.elevatorNumber = 3
      self._SetUpStatsTable(initStats)
      
   def _SetUpStatsTable(self, initStats):
      assert(not self.initialized)
      assert len(initStats) == self.elevatorNumber, \
             "Elevator count doesn't match %d" % self.elevatorNumber

      self.elevatorStats = {}
      for elevator, stats in initStats.iteritems():
         floor = stats['floor']
         users = stats.get('users', [])
         assert len(users) == 0, \
                "Elevator %d is not empty at beginning" % elevator
         assert (floor >=1 and floor <= 11), \
                ("Elevator %d malfunctioned, current position %d floor" % \
                 (elevator, floor))
         assert floor == 1, \
                ("Elevator %d doesn't stay at 1st floor but %d floor" % \
                 (elevator, floor))
         self.elevatorStats[elevator] = {
            'floor': 1,
            'moveUps': 0,
            'moveDowns': 0,
         }
      self.initialized = True

   def UpdateElevatorStats(self, ts, newStats):
      assert(self.initialized)
      assert len(newStats) == self.elevatorNumber, \
             ("Elevator count doesn't match %d at time %d" %\
              (self.elevatorNumber, ts))

      idle = True
      for elevator, stats in newStats.iteritems():
         users = stats.get('users', [])
         assert len(users) <= 20, \
                "Elevator %d overloaded at %d, 20 at most but %d users detected" % (elevator, ts, len(users))
         # Any elevator has passenger, means entire elevator system is not idle
         idle = (idle and len(users) == 0)

         floor = stats['floor']
         assert (floor >=1 and floor <= 11), \
                ("Elevator %d malfunctioned at time %d, current position %d floor" % \
                 (elevator, ts, floor))
         assert elevator in self.elevatorStats, \
                "Rogue elevator %d detected at %d" % (elevator, ts)
         existingStats = self.elevatorStats[elevator]
         assert abs(floor - existingStats['floor']) <= 1, \
                ("Elevator %d overspeed detected at %s, last position %d floor, current position %d floor" % \
                 (elevator, ts, existingStats['floor'], floor))
         if floor > existingStats['floor']:
            existingStats['moveUps'] += 1
         elif floor < existingStats['floor']:
            existingStats['moveDowns'] += 1
         else:
            # elevator stops for a time unit
            pass
         # Update elevator latest position
         existingStats['floor'] = floor
      # We need to figure out the acount of elevator system's continuous idle time.
      # This will be used to determine whether verification logic should stop.
      # For example, there are still users not served but already requested, but
      # elevator system has been idle for more than 20 time units(a round trip from
      # bottom to top takes 20 time units), it should indicate that elevator system
      # has gone bad(has bugs), we don't have to wait any further.
      if idle:
         self.idleCount +=1
      else:
         self.idleCount = 0

   def GeneratePowerReport(self):
      powerCost = 0
      for elevator, stats in self.elevatorStats.iteritems():
         powerCost += stats['moveUps'] * 0.6
         powerCost += stats['moveDowns'] * 0.4
      return powerCost
