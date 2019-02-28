# jenkins-shared-pipeline

How to use:
- Include shared pipeline into Jenkins via configuration settings
- Use in in Jenkinsfile with PROJECT (name of the helm release) and DOCKER_REGISTRY_NAME (name of repository at dockerhub (p1hub org)).
Sample:
```
@Library('p1pipeline')_

p1pipeline("p1storefront","storefront")
```
