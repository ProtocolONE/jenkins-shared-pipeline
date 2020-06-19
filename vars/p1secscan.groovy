def call() {
    echo "Running aqua microscanner"
    BR_NAME=URLDecoder.decode(env.BRANCH_NAME)
    BR_NAME=BR_NAME.replaceAll("/","-").replaceAll("_","-").replaceAll("#","").toLowerCase()
    registryImage=env.CI_REGISTRY_IMAGE
    if(env.JOB_NAME.indexOf("qilin/auth1.protocol.one")!=-1){
        registryImage=env.CI_REGISTRY_IMAGE+"-qilin"
    }

    //aquaMicroscanner imageName: "${registryImage}:${BR_NAME}-$BUILD_ID", notCompliesCmd: '', onDisallowed: 'ignore', outputFormat: 'html'
}
