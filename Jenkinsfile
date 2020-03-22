@Library('sd')_

def kubeLabel = getKubeLabel()

pipeline {

    agent {
        kubernetes {
            label "${kubeLabel}"
            cloud 'Kube mwdevel'
            defaultContainer 'runner'
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
                sh 'mvn clean package'
                sh 'cp target/cdmi-server-1.2.1.jar ./rpm/SOURCES'
                sh 'cp config/application.yml ./rpm/SOURCES'
            }
        }
        stage('package') {
            steps {
                dir('rpm') {
                    sh 'rpmbuild --define "_topdir $(pwd)" -ba $(pwd)/SPECS/cdmi-server.spec'
                    sh 'cp $(pwd)/RPMS/x86_64/*.rpm .'
                    archiveArtifacts '*.rpm'
                }
            }
        }
    }
}
