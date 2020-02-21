def call() {
    script {
        def repositoryUrl = scm.userRemoteConfigs[0].url

        def getBranches = ("git ls-remote -t -h ${repositoryUrl}").execute()

        def branchList = getBranches.text.readLines().collect {
                it.split()[1].replaceAll('refs/heads/', '').replaceAll('refs/tags/', '').replaceAll("\\^\\{\\}", '')
        }

        def selectBranch = input message: 'Please select branch', ok: 'Next',
            parameters: [
                [
                $class: 'ChoiceParameterDefinition',
                name: 'BRANCH', 
                choices: branchList,
                description: 'Project branches'
                ],
            ]

        def selectedBranch = sh (returnStdout: true, script: "echo ${selectBranch} | cut -d ' ' -f 1")
        echo "You selected branch with name: ${selectedBranch}"
        env.BRANCH_NAME=selectedBranch
    }
}
