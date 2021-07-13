#!/usr/bin/python
# based on the solution from https://stackoverflow.com/questions/28311030/check-jenkins-job-status-after-triggering-a-build-remotely
#
# USAGE: docker run -it -e JENKINS_AUTH_TOKEN='user:token' \
# docker run -it -e JENKINS_AUTH_TOKEN='user:token' \
#     -e JENKINS_BUILD_TOKEN='trigger_token' \
#     -e JENKINS_BUILD_PROJECT='jenkins_project' \
#     -e JENKINS_BUILD_BRANCH='master' \
#     p1hub/p1jenkinstrigger  
#
# optional env variable: JENKINS_URI, default value: jenkins.protocol.one

import requests
import re
import os
import sys 
import json
import time
import urllib.parse

# secs for polling Jenkins API
#
QUEUE_POLL_INTERVAL = 2 
JOB_POLL_INTERVAL = 5
OVERALL_TIMEOUT = 3600 # 1 hour

# by default deploy to tst environment and 
DEPLOY_TO_ENV = os.environ.get('DEPLOY_TO_ENV') or 'tst'
IS_RB = os.environ.get('IS_RB') or 'false'

# job specifics: should be passed in
jenkins_uri = os.environ.get('JENKINS_URI') or 'jenkins.protocol.one'
job_name = 'jobrunner'
auth_token = urllib.parse.quote(os.environ['JENKINS_AUTH_TOKEN'], safe=':')
build_token = urllib.parse.quote(os.environ['JENKINS_BUILD_TOKEN'])
build_project = urllib.parse.quote(os.environ['JENKINS_BUILD_PROJECT'], safe='/')
build_branch = urllib.parse.quote(os.environ['JENKINS_BUILD_BRANCH'], safe='')

post_payload = {'PROJECT': build_project, 'BRANCH': build_branch, 'DEPLOY_TO_ENV' : DEPLOY_TO_ENV, 'IS_RB': IS_RB }

# start the build
#
start_build_url = 'https://{}/generic-webhook-trigger/invoke?token={}'.format(jenkins_uri, build_token)
r = requests.post(start_build_url, data=json.dumps(post_payload))
print(r.text)

#queue_id =  r.json()['data']['triggerResults']['jobrunner']['id']
queue_id =  r.json()['jobs']['jobrunner']['id']

job_info_url = 'https://{}@{}/queue/item/{}/api/json'.format(auth_token, jenkins_uri, queue_id)
elasped_time = 0 
print('{} Job {} added to queue'.format(time.ctime(), job_name))
while True:
    print("{}: trying to get job id...".format(time.ctime()))
    try:
        l = requests.get(job_info_url,timeout=5)
        jqe = l.json()
        task = jqe['task']['name']
        job_id = jqe['executable']['number']
        break
    except:
        print("no job ID yet for build: {}".format(task))
        time.sleep(QUEUE_POLL_INTERVAL)
        elasped_time += QUEUE_POLL_INTERVAL

    if (elasped_time % (QUEUE_POLL_INTERVAL * 10)) == 0:
        print("{}: Job {} not started yet from queue id {}".format(time.ctime(), job_name, queue_id))

print("{}: Job started: https://{}/job/{}/{}/console".format(time.ctime(),jenkins_uri, job_name, job_id))

# poll job status waiting for a result
#
job_url = 'https://{}@{}/job/{}/{}/api/json'.format(auth_token, jenkins_uri, job_name, job_id)
start_epoch = int(time.time())
while True:
    print("{}: trying to get job status...".format(time.ctime()))
    result = ''
    try:
        j = requests.get(job_url,timeout=5)
        jje = j.json()
        result = jje['result']
    except:
        print("request timeout")    
    if result == 'SUCCESS':
        # Do success steps
        print("{}: Job: {} Status: {}".format(time.ctime(), job_name, result))
        break
    elif result == 'FAILURE':
        # Do failure steps
        print("{}: Job: {} Status: {}".format(time.ctime(), job_name, result))
        sys.exit(1)
    elif result == 'ABORTED':
        # Do aborted steps
        print("{}: Job: {} Status: {}".format(time.ctime(), job_name, result))
        sys.exit(1)
    else:
        print("{}: Job: {} Status: {}. Polling again in {} secs".format(time.ctime(), job_name, result, JOB_POLL_INTERVAL))
    
    cur_epoch = int(time.time())
    if (cur_epoch - start_epoch) > OVERALL_TIMEOUT:
        print("No status before timeout of {} secs".format(OVERALL_TIMEOUT))
        sys.exit(1)

    time.sleep(JOB_POLL_INTERVAL)
