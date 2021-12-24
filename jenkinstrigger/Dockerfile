FROM python:3.8-alpine

RUN apk update && apk upgrade

RUN pip install requests

COPY jenkins.py /opt/

ENTRYPOINT python /opt/jenkins.py
