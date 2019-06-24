pipeline {

    agent {
        kubernetes {
            label "cdmi-server-${env.JOB_BASE_NAME}-${env.BUILD_NUMBER}"
            cloud 'Kube mwdevel'
            defaultContainer 'jnlp'
            inheritFrom 'ci-template'
        }
    }

    options {
        timeout(time: 2, unit: 'HOURS')
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    triggers { cron('@daily') }

    stages {
        stage('build') {
            steps {
                container('runner'){
                    sh 'mvn clean package'
                    sh 'cp target/cdmi-server-1.2.1.jar ./rpm/SOURCES'
                    sh 'cp config/application.yml ./rpm/SOURCES'
                }
            }
        }
        stage('package') {
            steps {
                container('runner') {
                    dir('rpm') {
                        sh 'rpmbuild --define "_topdir $(pwd)" -ba $(pwd)/SPECS/cdmi-server.spec'
                        sh 'cp $(pwd)/RPMS/x86_64/*.rpm .'
                        archiveArtifacts '*.rpm'
                    }
                }
            }
        }
    }
}
