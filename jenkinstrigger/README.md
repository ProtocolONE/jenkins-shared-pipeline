# python-based docker image for remote triggering Jenkins build and reporting build status using Jenkins API
# 

Build and push:
```
docker build . -t p1hub/p1jenkinstrigger
docker push p1hub/p1jenkinstrigger
```

Usage: 
```
docker run -it -e JENKINS_AUTH_TOKEN='user:token' \
     -e JENKINS_BUILD_TOKEN='trigger_token' \
     -e JENKINS_BUILD_PROJECT='jenkins_project' \
     -e JENKINS_BUILD_BRANCH='master' \
     p1hub/p1jenkinstrigger  
```
Optional Environment variable: JENKKINS_URI, default value: jenkins.protocol.one