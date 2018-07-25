"""
Copyright (c) 2018 VMware, Inc.  All rights reserved.
-- VMware Confidential
"""

import random
import os
from os.path import join
import datetime

# This is used to generate random elevator workloads in specific scenario.

def SimulateMorning(startTime, endTime, firstIndex, totalCount):
   workloads = []
   # In the morning, start floor is always 1
   startFloor = 1
   for index in range(firstIndex, firstIndex + totalCount):
      ts = random.randint(startTime, endTime)
      # generate a random destination
      destFloor = random.randint(2, 11)
      workloads.append((ts, index, startFloor, destFloor))
   return workloads

def SimulateEvening(startTime, endTime, firstIndex, totalCount):
   workloads = []
   # In the evening, dest floor is always 1
   destFloor = 1
   for index in range(firstIndex, firstIndex + totalCount):
      ts = random.randint(startTime, endTime)
      # generate a random destination
      startFloor = random.randint(2, 11)
      workloads.append((ts, index, startFloor, destFloor))
   return workloads

def SimulateNoonUp(startTime, endTime, firstIndex, totalCount):
   workloads = []
   # At noon, start floor is 1 or 2
   startFloor = random.randint(1, 2)
   for index in range(firstIndex, firstIndex + totalCount):
      ts = random.randint(startTime, endTime)
      # generate a random destination
      destFloor = random.randint(startFloor + 1, 11)
      workloads.append((ts, index, startFloor, destFloor))
   return workloads

def SimulateNoonDown(startTime, endTime, firstIndex, totalCount):
   workloads = []
   # At noon, dest floor is 1 or 2
   destFloor = random.randint(1, 2)
   for index in range(firstIndex, firstIndex + totalCount):
      ts = random.randint(startTime, endTime)
      # generate a random destination
      startFloor = random.randint(destFloor + 1, 11)
      workloads.append((ts, index, startFloor, destFloor))
   return workloads

def SimulateRandom(startTime, endTime, firstIndex, totalCount):
   workloads = []
   for index in range(firstIndex, firstIndex + totalCount):
      ts = random.randint(startTime, endTime)
      startFloor = random.randint(1, 11)
      # generate a random destination
      destFloor = 0
      while(destFloor in [0, startFloor]):
         destFloor = random.randint(1, 11)
      workloads.append((ts, index, startFloor, destFloor))
   return workloads

def SourceOfChaos(load):
   t1, u1, t2, u2, t3, u3, t4, u4 = load
   workloads = SimulateMorning(1, t1, 0, u1)
   workloads += SimulateNoonDown(t1 + 1, t2, u1 + 1, u2)
   workloads += SimulateNoonUp(t1 + 1, t3, u1 + u2 + 1, u3)
   workloads += SimulateEvening(t3 + 1, t4, u1 + u2 + u3 + 1, u4)
   results = []
   for load in workloads:
      results.append("%d,%d,%d,%d" % load)
   return results

predefinedWorkloadPreffix = "predefined_workload_%s"
randomWorkloadPreffix = "random_workload_%s"

if __name__ == '__main__':
   destFile = predefinedWorkloadPreffix % datetime.datetime.now().microsecond
   workloads = SimulateMorning(1, 50, 0, 200)
   workloads += SimulateNoonDown(151, 250, 201, 50)
   workloads += SimulateNoonUp(151, 250, 251, 50)
   workloads += SimulateEvening(501, 700, 301, 200)
   # sort by timestamp
   with open(destFile, 'w') as fp:
      for load in sorted(workloads, key=lambda l:l[0]):
         fp.write("%d,%d,%d,%d\n" % load)

   print("Pre-defined workload file %s" % destFile)

   destFile = randomWorkloadPreffix % datetime.datetime.now().microsecond
   workloads = SimulateRandom(1, 5000, 0, 700)
   # sort by timestamp
   with open(destFile, 'w') as fp:
      for load in sorted(workloads, key=lambda l:l[0]):
         fp.write("%d,%d,%d,%d\n" % load)
   print("Pure random workload file %s" % destFile)
