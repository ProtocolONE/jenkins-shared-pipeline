FROM python:3.7-alpine

RUN pip install requests

COPY jenkins.py /opt/

ENTRYPOINT python /opt/jenkins.py
