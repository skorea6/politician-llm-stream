pipeline{
    agent any
    environment {
        SCRIPT_PATH = '/root/spring-main'
        SERVER_USER = 'root'
        SSH_OPTS = '-o StrictHostKeyChecking=no'
    }
    tools {
        gradle 'gradle 8.6'
    }
    stages{
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Get Commit Message') {
            steps {
                script {
                    def gitCommitMessage = sh(
                        script: "git log -1 --pretty=%B",
                        returnStdout: true
                    ).trim()
                    echo "Commit Message: ${gitCommitMessage}"
                    env.GIT_COMMIT_MESSAGE = gitCommitMessage
                }
            }
        }
        stage('Prepare'){
            steps {
                sh 'gradle clean'
            }
        }
        stage('Replace Prod Properties') {
            steps {
                withCredentials([file(credentialsId: 'politicianLLMStreamProd', variable: 'politicianLLMStreamProd')]) {
                    script {
                        sh 'cp $politicianLLMStreamProd ./src/main/resources/application-prod.yml'
                    }
                }
            }
        }
        stage('Build') {
            steps {
                sh 'gradle build -x test'
            }
        }
        stage('Test') {
            steps {
                sh 'gradle test'
            }
        }
        stage('Deploy') {
            steps {
                sshagent(credentials: ['politicianLLMStream_pem_key']) {
                    script {
                        def serverIPs = env.LLMSTREAM_SERVER_IPS.split(',')
                        serverIPs.each { ip ->
                            sh """
                                scp ${SSH_OPTS} ./docker/docker-compose.blue.yml ${SERVER_USER}@${ip}:${SCRIPT_PATH}
                                scp ${SSH_OPTS} ./docker/docker-compose.green.yml ${SERVER_USER}@${ip}:${SCRIPT_PATH}
                                scp ${SSH_OPTS} ./docker/Dockerfile ${SERVER_USER}@${ip}:${SCRIPT_PATH}
                                scp ${SSH_OPTS} ./scripts/deploy.sh ${SERVER_USER}@${ip}:${SCRIPT_PATH}
                                scp ${SSH_OPTS} ./build/libs/*.jar ${SERVER_USER}@${ip}:${SCRIPT_PATH}
                                ssh ${SSH_OPTS} ${SERVER_USER}@${ip} chmod +x ${SCRIPT_PATH}/deploy.sh
                                ssh ${SSH_OPTS} ${SERVER_USER}@${ip} ${SCRIPT_PATH}/deploy.sh
                            """
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            slackSend (
                message: "성공: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' 최근 커밋: '${env.GIT_COMMIT_MESSAGE}' (${env.BUILD_URL})",
            )
        }
        failure {
            slackSend (
                message: "실패: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' 최근 커밋: '${env.GIT_COMMIT_MESSAGE}' (${env.BUILD_URL})",
            )
        }
    }
}