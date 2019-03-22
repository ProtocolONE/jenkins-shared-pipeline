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

# secs for polling Jenkins API
#
QUEUE_POLL_INTERVAL = 2 
JOB_POLL_INTERVAL = 5
OVERALL_TIMEOUT = 3600 # 1 hour

# job specifics: should be passed in
jenkins_uri = os.environ.get('JENKINS_URI') or 'jenkins.protocol.one'
job_name = 'jobrunner'
auth_token = os.environ['JENKINS_AUTH_TOKEN']
build_token = os.environ['JENKINS_BUILD_TOKEN']
build_project = os.environ['JENKINS_BUILD_PROJECT']
build_branch = os.environ['JENKINS_BUILD_BRANCH']

post_payload = {'PROJECT': build_project, 'BRANCH': build_branch}

# start the build
#
start_build_url = 'https://{}/generic-webhook-trigger/invoke?token={}'.format(jenkins_uri, build_token)
r = requests.post(start_build_url, data=json.dumps(post_payload))
print(r.text)

queue_id =  r.json()['data']['triggerResults']['jobrunner']['id']

job_info_url = 'https://{}@{}/queue/item/{}/api/json'.format(auth_token, jenkins_uri, queue_id)
elasped_time = 0 
print('{} Job {} added to queue'.format(time.ctime(), job_name))
while True:
    l = requests.get(job_info_url)
    jqe = l.json()
    task = jqe['task']['name']
    try:
        job_id = jqe['executable']['number']
        break
    except:
        print("no job ID yet for build: {}".format(task))
        time.sleep(QUEUE_POLL_INTERVAL)
        elasped_time += QUEUE_POLL_INTERVAL

    if (elasped_time % (QUEUE_POLL_INTERVAL * 10)) == 0:
        print("{}: Job {} not started yet from queue id {}".format(time.ctime(), job_name, queue_id))

print('{}: Job started'.format(time.ctime()))
		
# poll job status waiting for a result
#
job_url = 'https://{}@{}/job/{}/{}/api/json'.format(auth_token, jenkins_uri, job_name, job_id)
start_epoch = int(time.time())
while True:
    j = requests.get(job_url)
    jje = j.json()
    result = jje['result']
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
