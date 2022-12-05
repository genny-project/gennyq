pipeline {
    agent any
    options {
        ansiColor('xterm')
    }
    stages {
        stage("Clone") {
            steps {
                git branch: "${BRANCH_TO_BUILD}",
                    url: 'https://github.com/genny-project/gennyq'
            }
        }
        stage("Build Dependencies") {
            steps {
                sh "./build.sh"
            }
        }
        stage("Build Services") {
            steps {
                parallel (
                    "Build GadaQ" : {
						sh "./build-docker.sh gadaq ${VERSION_TO_BUILD}"
                    },
					"Build Bridge" : {
						sh "./build-docker.sh bridge ${VERSION_TO_BUILD}"
					},
					"Build Fyodor" : {
						sh "./build-docker.sh fyodor ${VERSION_TO_BUILD}"
					},
					"Build Dropkick" : {
						sh "./build-docker.sh dropkick ${VERSION_TO_BUILD}"
					},
					"Build Lauchy" : {
						sh "./build-docker.sh lauchy ${VERSION_TO_BUILD}"
					},
					"Build Messages" : {
						sh "./build-docker.sh messages ${VERSION_TO_BUILD}"
					},
					"Build Shleemy" : {
						sh "./build-docker.sh shleemy ${VERSION_TO_BUILD}"
                    }
                )
            }
        }
        stage("Push Docker Images") {
            steps {
                sh "./push-docker.sh ${VERSION_TO_BUILD}"
            }
        }
    }
}
