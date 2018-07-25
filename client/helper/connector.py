"""
Copyright (c) 2018 VMware, Inc.  All rights reserved.
-- VMware Confidential
"""

import urllib2
import urllib
import json
import time

# This class is used to access elevator control service,
# which can help to:
# 1. reset elevator status;
# 2. send user request to elevator;
class ElevatorConnector(object):

   # url, the elevator service endpoint
   def __init__(self, url):
      self.url = url
      self.reset = False

   # send reset request to elevator service, expects:
   # 1. reset world clock;
   # 2. elevators go back to initial position
   # 3. clear all pending stats and jobs at service side;
   # This is expected to be called at the very beginning
   # of elevator verification.
   def ResetElevator(self):
      initState = self._Request_("reset")
      self.reset = True
      return initState

   def RequestElevator(self, workloads=None):
      assert(self.reset)
      return self._Request_("workload", workloads)

   def _Request_(self, path, data=None):
      response = None
      try:
         requestedUrl = "%s/%s" % (self.url, path)
         params = None
         if data:
            params = json.dumps(data)
         proxy_handler = urllib2.ProxyHandler({})
         opener = urllib2.build_opener(proxy_handler)
         headers = {'Content-Type': 'application/json'}
         req = urllib2.Request(requestedUrl, headers = headers, data = params)
         #req = urllib2.Request(requestedUrl, params)
         response = opener.open(req)
         return json.loads(response.read())
      finally:
         if response:
            response.close()