def call() {
    echo "Running aqua microscanner"
    aquaMicroscanner imageName: "$CI_REGISTRY_IMAGE:$BRANCH_NAME-$BUILD_ID", notCompliesCmd: '', onDisallowed: 'ignore', outputFormat: 'html'
}
