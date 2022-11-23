def call() {
    pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'pip install -r ./audit_log/requirements.txt'
                sh 'pip install -r ./health/requirements.txt'
                sh 'pip install -r ./processing/requirements.txt'
                sh 'pip install -r ./reciever/requirements.txt'
                sh 'pip install -r ./storage/requirements.txt'
            }
        }
        stage('Static Code Checking') {
            steps {
                script {
                    sh 'pylint-fail-under --fail_under 5.0 **/*.py'
                }
            }
        }
        stage('Building') {
            steps {
                withCredentials([string(credentialsId: 'DockerHub', variable: 'TOKEN')]) {
                    sh "docker login -u 'jovan9876' -p '$TOKEN' docker.io"
                    sh "docker build -t reciever:latest -f reciever.Dockerfile --tag jovan9876/reciever:reciever ."
                    sh "docker push jovan9876/reciever:reciever"
                    sh "docker build -t storage:latest -f storage.Dockerfile --tag jovan9876/storage:storage ."
                    sh "docker push jovan9876/storage:storage"
                    sh "docker build -t processing:latest -f processing.Dockerfile --tag jovan9876/processing:processing ."
                    sh "docker push jovan9876/processing:processing"
                    sh "docker build -t audit_log:latest -f audit_log.Dockerfile --tag jovan9876/audit_log:audit_log ."
                    sh "docker push jovan9876/audit_log:audit_log"
                }
            }
        }
        stage('Zip Artifacts') {
            steps {
                sh 'zip -r reciever.zip reciever/'
                sh 'zip -r storage.zip storage/'
                sh 'zip -r processing.zip processing/'
                sh 'zip -r audit_log.zip audit_log/'
            }
            post {
                always {
                    archiveArtifacts 'reciever.zip'
                    archiveArtifacts 'storage.zip'
                    archiveArtifacts 'processing.zip'
                    archiveArtifacts 'audit_log.zip'
                } 
            }
        }
        def remote = [:]
        remote.name = "ACIT3855"
        remote.host = "acit-3855.eastus.cloudapp.azure.com"
        remote.allowAnyHosts = true
        remote.user = 'azureuser'
        stage('Deploy') {
            steps {
                echo 'Deploying....'
                    withCredentials([sshUserPrivateKey(credentialsId: 'sshUser', keyFileVariable: 'identity', passphraseVariable: '')]) {

                    remote.identityFile = identity
                    stage("SSH Steps Rocks!") {
                        writeFile file: 'abc.sh', text: 'ls'
                        sshCommand remote: remote, command: 'for i in {1..5}; do echo -n \"Loop \$i \"; date ; sleep 1; done'
                        sshPut remote: remote, from: 'abc.sh', into: '.'
                        sshGet remote: remote, from: 'abc.sh', into: 'bac.sh', override: true
                        sshScript remote: remote, script: 'abc.sh'
                        sshRemove remote: remote, path: 'abc.sh'
                    }
            }
        }
    }
  }
}

