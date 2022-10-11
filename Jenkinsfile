pipeline {
    agent any
    options {
        ansiColor('xterm')
    }
    stages {
        stage('Add Config files') {
            steps {
                configFileProvider([configFile(fileId: '53b50115-91ad-42e2-88e3-07a292f05b14', targetLocation: 'ports')]) {
                    sh ". ./ports"
                }
            }
        }
        stage("Clone") {
            steps {
                git branch: "${BRANCH_TO_BUILD}",
                    url: 'https://github.com/genny-project/gennyq'
            }
        }
        stage("Build Dependencies") {
            steps {
                configFileProvider([configFile(fileId: '53b50115-91ad-42e2-88e3-07a292f05b14', targetLocation: 'ports')]) {
                    sh ". ./ports"
                }
                sh "./build.sh"
            }
        }
        stage("Build Services") {
            steps {
                parallel (
                    "Build GadaQ" : {
                        configFileProvider([configFile(fileId: '53b50115-91ad-42e2-88e3-07a292f05b14', targetLocation: 'ports')]) {
                            sh ". ./ports"
                            sh "./build-docker.sh gadaq"
                            sh "./mvnw clean" 
                        }
                    },
                    "Build Bridge" : {
                        configFileProvider([configFile(fileId: '53b50115-91ad-42e2-88e3-07a292f05b14', targetLocation: 'ports')]) {
                            sh ". ./ports"
                            sh "./build-docker.sh bridge"
                            sh "./mvnw clean" 
                        }
                    },
                    "Build Fyodor" : {
                        configFileProvider([configFile(fileId: '53b50115-91ad-42e2-88e3-07a292f05b14', targetLocation: 'ports')]) {
                            sh ". ./ports"
                            sh "./build-docker.sh fyodor"
                            sh "./mvnw clean" 
                        }
                    },
                    "Build Dropkick" : {
                        configFileProvider([configFile(fileId: '53b50115-91ad-42e2-88e3-07a292f05b14', targetLocation: 'ports')]) {
                            sh ". ./ports"
                            sh "./build-docker.sh dropkick"
                            sh "./mvnw clean" 
                        }
                    },
                    "Build Lauchy" : {
                        configFileProvider([configFile(fileId: '53b50115-91ad-42e2-88e3-07a292f05b14', targetLocation: 'ports')]) {
                            sh ". ./ports"
                            sh "./build-docker.sh lauchy"
                            sh "./mvnw clean" 
                        }
                    },
                    "Build Messages" : {
                        configFileProvider([configFile(fileId: '53b50115-91ad-42e2-88e3-07a292f05b14', targetLocation: 'ports')]) {
                            sh ". ./ports"
                            sh "./build-docker.sh messages"
                            sh "./mvnw clean" 
                        }
                    },
                    "Build Shleemy" : {
                        configFileProvider([configFile(fileId: '53b50115-91ad-42e2-88e3-07a292f05b14', targetLocation: 'ports')]) {
                            sh ". ./ports"
                            sh "./build-docker.sh shleemy"
                            sh "./mvnw clean" 
                        }
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
    post {
    // Clean after build
    always {
        cleanWs(cleanWhenNotBuilt: false,
                deleteDirs: true,
                disableDeferredWipeout: true,
                notFailBuild: true)
        }
    }
}
