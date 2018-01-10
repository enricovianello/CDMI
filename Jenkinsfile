pipeline {
    agent none

    options {
        timeout(time: 2, unit: 'HOURS')
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    triggers { cron('@daily') }

    parameters { string(name: 'BRANCH_NAME', defaultValue: 'master') }

    stages {
        stage('build') {
            agent { label 'maven' }
            steps {
                container('maven-runner'){
                    git(url: 'https://github.com/enricovianello/CDMI.git', branch: "${params.BRANCH_NAME}")
                    sh 'mvn clean package'
                    sh 'cp target/cdmi-server-1.2.1.jar ./rpm/SOURCES'
                    sh 'cp config/application.yml ./rpm/SOURCES'
                    dir("rpm") {
                        stash name: "cdmi-rpm-dir", includes: "**"
                    }
                }
            }
        }
        stage('package') {
            agent { label 'generic' }
            steps {
                container('generic-runner'){
                    unstash name: 'cdmi-rpm-dir'
                    sh 'rpmbuild --define "_topdir $(pwd)" -ba $(pwd)/SPECS/cdmi-server.spec'
                    archiveArtifacts '*.rpm'
                }
            }
        }
    }
}
