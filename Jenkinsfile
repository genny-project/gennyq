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
						sh "./build-docker.sh gadaq"
                    },
					"Build Bridge" : {
						sh "./build-docker.sh bridge"
					},
					"Build Fyodor" : {
						sh "./build-docker.sh fyodor"
					},
					"Build Dropkick" : {
						sh "./build-docker.sh dropkick"
					},
					"Build Lauchy" : {
						sh "./build-docker.sh lauchy"
					},
					"Build Messages" : {
						sh "./build-docker.sh messages"
					},
					"Build Shleemy" : {
						sh "./build-docker.sh shleemy"
                    }
                )
            }
        }
        stage("Push Docker Images") {
            steps {
                sh "./push-docker.sh"
            }
        }
    }
}
