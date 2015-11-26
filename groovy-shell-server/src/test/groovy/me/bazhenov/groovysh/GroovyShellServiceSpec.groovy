package me.bazhenov.groovysh

import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class GroovyShellServiceSpec extends Specification {

    def "default port is 6789"() {
        given:
        def gss = new GroovyShellService()

        expect:
        gss.port == 6789
    }

    def "IllegalArgumentException for port #port from range 1-65535"() {
        when:
        new GroovyShellService(port as int)

        then:
        thrown(IllegalArgumentException)

        where:
        port << [-1, 0, 65536, Integer.MAX_VALUE]
    }

    def "no exception for port #port from range 1-65535"() {
        when:
        new GroovyShellService(port)

        then:
        noExceptionThrown()

        where:
        port << [1, 8888, 65535]
    }

    def "add default script will not remove existing scripts"() {
        when:
        def gss = new GroovyShellService()
        gss.setDefaultScripts(['script1', 'script2', 'script3'])

        then:
        gss.getDefaultScripts() == ['script1', 'script2', 'script3']

        when:
        gss.addDefaultScript('new_script')

        then:
        gss.getDefaultScripts() == ['script1', 'script2', 'script3', 'new_script']
    }

}
