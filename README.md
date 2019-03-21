# jenkins-shared-pipeline

How to use:
- Include shared pipeline into Jenkins via configuration settings
- Use it in Jenkinsfile with PROJECT (name of the helm release) and DOCKER_REGISTRY_NAME (name of repository at dockerhub (p1hub org)).
Sample:
```
@Library('p1pipeline')_

    "p1auth1", //helm release name
    "p1auth1", // docker hub registry
    "develop", // development branch for test releases
    "dev", // kubernetes namespace for test releases
    "dev-" // domain name prefix in kubernetes for test releases
```
