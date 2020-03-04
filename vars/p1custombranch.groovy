@NonCPS
def call() {
    script {
        def repositoryUrl = scm.userRemoteConfigs[0].url

        def getBranches = ("git ls-remote -t -h ${repositoryUrl}").execute()

        def BranchList = getBranches.text.readLines().collect {
                it.split()[1].replaceAll('refs/heads/', '').replaceAll('refs/tags/', '').replaceAll("\\^\\{\\}", '')
        }
        echo "BranchList: ${BranchList}"
        
        def selectBranch = input message: 'Please select branch', ok: 'Next',
            parameters: [
                [
                $class: 'ChoiceParameterDefinition',
                name: 'BRANCH', 
                choices: BranchList,
                description: 'Project branches'
                ]
            ]

        def selectedBranch = sh (returnStdout: true, script: "echo ${selectBranch}")
        echo "You selected branch with name: ${selectBranch}"
        //env.BRANCH_NAME=selectedBranch
    }
}
