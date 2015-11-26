package me.bazhenov.groovysh.rules

import org.hidetake.groovy.ssh.Ssh
import org.junit.rules.ExternalResource

class SshClientRule extends ExternalResource {

    def ssh = Ssh.newService()

    def setCredentials(String user, String password) {
        ssh.settings.user = user
        ssh.settings.password = password
    }

    @Override
    protected void before() throws Throwable {
        ssh.settings {
            knownHosts = allowAnyHosts
            user = "test_user"

        }
        ssh.remotes {
            testServer {
                host = '127.0.0.1'
                port = 6789
            }
        }
    }

    def executeCommands(String... commands = []) {
        def output = new SessionOutput()
        ssh.run {
            session(ssh.remotes.testServer) {
                shell outputStream: output, interaction: {
                    commands.each { command ->
                        standardInput << command << "\n"
                    }
                    standardInput << ":exit" << "\n"
                }
            }
        }
        output
    }

}
