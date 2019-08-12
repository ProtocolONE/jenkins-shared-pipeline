def call() {
    echo "Running aqua microscanner"
    BR_NAME=env.BRANCH_NAME
    BR_NAME=BR_NAME.replaceAll("/","-")
    aquaMicroscanner imageName: "$CI_REGISTRY_IMAGE:${BR_NAME}-$BUILD_ID", notCompliesCmd: '', onDisallowed: 'ignore', outputFormat: 'html'
}
