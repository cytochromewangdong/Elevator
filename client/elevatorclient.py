"""
Copyright (c) 2018 VMware, Inc.  All rights reserved.
-- VMware Confidential
"""

import sys
import getopt
from helper.connector import ElevatorConnector
from helper.loadgenerator import WorkLoadGenerator
from helper.monitor import ElevatorMonitor
from generator.generator import SourceOfChaos
import random

usage = """Load specified workload and verify specified elevator service

Usage: elevatorclient.py [options]

Options:
    -h / --help
        Print this message and exit.

    -S servicename
    --servicename=servicename
        The endpoint of elevator service (required field)

    -F file
    --file=file
        The path of workload file (optional field). If omitted, the default dummy file will be taken
"""

malfunctionMsg = """All elevators have been in idle state for more than 20 time units,
but there are still %d users staying in aisle who have already requested."""

intenralScoreMsg = """Workload execution time: %d time units
Actual power cost: %d units
Average customer satisfaction rate: %.3f%%
Total score: %.2f
"""

scoreMsg = """Average customer satisfaction rate: %.3f%%
Total score: %.2f
"""

fakeWorkloads = [
(50, 110, 170, 155, 170, 180, 440, 200),
(60, 133, 155, 155, 155, 180, 450, 200),
(70, 167, 170, 155, 170, 180, 460, 200),
(80, 193, 223, 155, 223, 180, 470, 200),
(90, 207, 199, 155, 199, 180, 480, 200),
(85, 222, 245, 155, 245, 180, 490, 200),
(75, 289, 170, 155, 170, 180, 500, 200),
(65, 199, 151, 155, 151, 180, 510, 200),
(55, 100, 170, 155, 170, 180, 520, 200),
(63, 109, 175, 155, 175, 180, 530, 200),
(73, 148, 168, 155, 168, 180, 540, 200),
(83, 167, 350, 155, 350, 180, 550, 200),
(93, 179, 332, 155, 332, 180, 560, 200),
(40, 278, 288, 155, 288, 180, 570, 200),
(45, 266, 177, 155, 177, 180, 580, 200),
(63, 109, 175, 155, 175, 180, 530, 200),
(73, 148, 168, 155, 168, 180, 540, 200),
(83, 167, 350, 155, 350, 180, 550, 200),
(93, 179, 332, 155, 332, 180, 560, 200),
(40, 278, 288, 155, 288, 180, 570, 200),
(45, 266, 177, 155, 177, 180, 580, 200),
]

def Usage():
    print(usage)

def Score(ts, monitor, workloader):
   worstPowerCost, avgSatisfactionRate = workloader.GetSummary()
   actPowerCost = monitor.GeneratePowerReport()
   executionTime = ts
   if avgSatisfactionRate > 0:
      finalRate = avgSatisfactionRate * worstPowerCost / actPowerCost
   else:
      finalRate = 0
   finalScore = finalRate * 70 + 30
   #return (internalScoreMsg %
   #        (executionTime, actPowerCost, avgSatisfactionRate * 100, finalScore))
   return (scoreMsg %
           (avgSatisfactionRate * 100, finalScore))

def main():
   try:
      opts, args = getopt.getopt(sys.argv[1:],"hS:F:",["help","servicename=", "file="])
   except getopt.error, msg:
       # print help information and return:
       Usage()
       return(1)
   service = None
   workload = None
   for opt, arg in opts:
      if opt in ("-h","--help"):
         Usage()
         return(0)

      if opt in ("-S","--servicename"):
         service = arg

      if opt in ("-F", "--file"):
         workload = arg

   if service is None:
      Usage()
      return(1)

   if not service.startswith("http://"):
      service = "http://%s" % service
   conn = ElevatorConnector(service)

   # randomly pick a place, to send real workload
   fact = random.randint(1, 3)
   score = None
   for i in range(3):
      print "i="+str(i)
      if (i + 1) == fact:
	 print "load work:"+workload
         loadGenerator = WorkLoadGenerator(workload)
         #loadGenerator = WorkLoadGenerator(filePath=None, plan=SourceOfChaos(fakeWorkloads[i]))
      else:
         print "load random workload"
         loadGenerator = WorkLoadGenerator(filePath=None, plan=SourceOfChaos(fakeWorkloads[i]))
      worldClock = 0
      monitor = None
      maxWorldClock = 0
      while(not loadGenerator.IsDone()):
         try:
            if worldClock == 0:
               newState = conn.ResetElevator()
               newState = DecodeStats(newState)
               monitor = ElevatorMonitor(newState)
               maxWorldClock = loadGenerator.maxWorldClock
               continue
            workloads = loadGenerator.GenNext(worldClock, newState)
            newState = conn.RequestElevator(workloads)
            newState = DecodeStats(newState)
            monitor.UpdateElevatorStats(worldClock, newState)
            assert worldClock <= maxWorldClock, \
                   "Elevators should have finished the tasks." 
         finally:
            worldClock += 1
      if (i + 1) == fact:
         score = Score(worldClock, monitor, loadGenerator)
   print(score)

def DecodeStats(elevatorStats):
   finalStats = {}
   for elevator, stats in elevatorStats.iteritems():
      floor = stats['floor']
      users = stats.get('users', [])
      newStats = {
         'floor': int(floor),
         'users': []
      }
      for u in users:
         newStats['users'].append(int(u))
      finalStats[int(elevator)] = newStats
   return finalStats

if __name__ == '__main__':
   main()
