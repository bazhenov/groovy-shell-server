package me.bazhenov.groovysh

import com.jcraft.jsch.JSchException
import me.bazhenov.groovysh.rules.SshClientRule
import org.apache.sshd.common.Session
import org.apache.sshd.server.session.ServerSession
import org.junit.ClassRule
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class GroovyShellServiceIntegrationSpec extends Specification {

    @ClassRule
    @Shared
    SshClientRule ssh = new SshClientRule()

    @Shared
    def groovyShellService = new GroovyShellService()

    def cleanup() {
        groovyShellService.destroy()
    }

    def "shell has groovysh basic features"() {
        given:
        groovyShellService.start()

        when:
        def output = ssh.executeCommands(command)

        then:
        output.contains(result)

        where:
        command     | result
        "1 + 1"     | "===> 2"
        ":help"     | ":help      (:h ) Display this help message"
    }

    def "default script is executed when session starts"() {
        given:
        groovyShellService.addDefaultScript("target/test-classes/scripts/scriptfile")
        groovyShellService.start()

        when:
        def output = ssh.executeCommands()

        then:
        output.contains("This is script!!!")
    }

    def "registered bindings are available"() {
        given:
        def greeter = new Greeter("Aleksandra")
        Map<String, Object> bindings = new HashMap<>()
        bindings.put("greeter", greeter)
        groovyShellService.setBindings(bindings)
        groovyShellService.start()

        when:
        def output = ssh.executeCommands("greeter.hello()")

        then:
        output.contains("===> Greetings Aleksandra!!!")
    }

    def "default script can use bindings"() {
        given:
        def greeter = new Greeter("Aleksandra")
        Map<String, Object> bindings = new HashMap<>()
        bindings.put("greeter", greeter)
        groovyShellService.setBindings(bindings)
        groovyShellService.addDefaultScript("target/test-classes/scripts/scriptfile")
        groovyShellService.start()

        when:
        def output = ssh.executeCommands("greeter.hello()")

        then:
        output.contains("===> Greetings Aleksandra!!!")
    }

    def "password authenticator set"() {
        given:
        def authenticator = { String username, String password, ServerSession session ->
            username == "Amiga" && password == "bestComputerEver!"
        }
        groovyShellService.setPasswordAuthenticator(authenticator)
        groovyShellService.start()

        when:
        ssh.executeCommands()

        then:
        def ex = thrown(JSchException)
        ex.message == "Auth fail"

        when:
        ssh.setCredentials("Amiga", "bestComputerEver!")
        def output = ssh.executeCommands("1 + 1")

        then:
        output.contains("===> 2")
    }

    def "thread factory set"() {
        given:
        def createdByFactory = false
        def threadFactory = { Runnable runnable, Session session ->
            createdByFactory = true
            new Thread(runnable)
        }
        groovyShellService.setThreadFactory(threadFactory)
        groovyShellService.start()

        when:
        ssh.executeCommands()

        then:
        createdByFactory
    }

}
