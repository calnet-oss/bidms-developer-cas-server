/*
 * Copyright (c) 2022, Regents of the University of California and
 * contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.berkeley.calnet.bidms.cas

import io.github.bkoehm.apacheds.embedded.EmbeddedLdapServer
import org.apereo.cas.configuration.CasConfigurationProperties
import org.apereo.cas.configuration.model.support.ldap.AbstractLdapAuthenticationProperties
import org.apereo.cas.util.model.TriStateBoolean
import org.apereo.cas.web.CasWebApplication
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import spock.lang.Shared
import spock.lang.Specification

@SpringBootTest(classes = [CasWebApplication], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class BidmsCasIntegrationSpec extends Specification {

    @Autowired
    CasConfigurationProperties casConfig
    @Autowired
    EmbeddedLdapServer embeddedLdapServer
    @Autowired
    RestTemplateBuilder restTemplateBuilder

    @LocalServerPort
    int serverPort

    @Value('${server.contextPath}')
    String contextPath

    TestRestTemplate restTemplate

    @Shared
    TestWebappExecutor testWebappExecutor

    @Shared
    URI baseUrl

    URI casBaseUrl

    void setupSpec() {
        this.testWebappExecutor = new TestWebappExecutor(8070)
        testWebappExecutor.run()
        this.baseUrl = new URI("https://localhost:${testWebappExecutor.portNumber}")
    }

    void cleanupSpec() {
        testWebappExecutor.shutdown()
    }

    void setup() {
        this.restTemplate = new TestRestTemplate(restTemplateBuilder, null, null, TestRestTemplate.HttpClientOption.SSL)
        this.casBaseUrl = new URI("https://localhost:${serverPort}" + (contextPath != '/' ? contextPath : ''))
    }

    // Confirm the YAML is binding to the Java configuration objects
    void "check configuration"() {
        expect:
        with(casConfig) {
            !authn.accept.users
            with(server) {
                name == "https://localhost:$serverPort" // 8060
                prefix.endsWith(contextPath) // /cas
            }
            with(authn.ldap[0]) {
                type == AbstractLdapAuthenticationProperties.AuthenticationTypes.AUTHENTICATED
                ldapUrl == "ldap://localhost:10389"
                baseDn == "dc=mydomain,dc=org"
                bindDn == "uid=admin,ou=system"
                bindCredential == "secret"
                principalAttributeId == "uid"
                additionalAttributes[0] == "cn"
                searchFilter == "cn={user}"
                connectTimeout == "PT5S"
                subtreeSearch
                !allowMissingPrincipalAttributeValue
                !allowMultiplePrincipalAttributeValues
                !allowMultipleDns
            }
            with(personDirectory) {
                principalAttribute == "uid"
                returnNull == TriStateBoolean.FALSE
                principalResolutionFailureFatal
            }
            with(authn.attributeRepository.ldap[0]) {
                with(attributes) {
                    uid == "uid"
                    sn == "sn"
                    givenName == "givenName"
                    cn == "cn"
                }
                ldapUrl == "ldap://localhost:10389"
                connectTimeout == "PT5S"
                baseDn == "dc=mydomain,dc=org"
                searchFilter == "cn={user}"
                subtreeSearch
                bindDn == "uid=admin,ou=system"
                bindCredential == "secret"
            }
        }
    }

    void "test that test webapp is CAS-protecting the endpoint URL"() {
        when:
        def response = restTemplate.getForEntity("${baseUrl}/protected", String)
        println "statusCode=${response.statusCode}, headers=${response.headers}"

        then:
        response.statusCode == HttpStatus.FOUND
        response.headers.getFirst(HttpHeaders.LOCATION).startsWith("${casBaseUrl}/login?service=")
    }

    void "test authenticating to CAS-protected endpoint"() {
        when: "get login URL"
        def response = restTemplate.getForEntity("${baseUrl}/protected", String)
        println "getLoginUrl statusCode=${response.statusCode}, headers=${response.headers}"
        assert response.statusCode == HttpStatus.FOUND
        def loginUrl = response.headers.getFirst(HttpHeaders.LOCATION)
        println "Using loginUrl $loginUrl"

        and: "retrieve login page content"
        HttpHeaders headers = new HttpHeaders().with {
            setAccept([MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML])
            add(HttpHeaders.HOST, "${this.casBaseUrl.host}:${this.casBaseUrl.port}")
            it
        }
        println "Sending with headers $headers"
        response = restTemplate.exchange(new RequestEntity<String>(headers, HttpMethod.GET, new URI(loginUrl)), String)
        println "retrieveLoginPage statusCode=${response.statusCode}, headers=${response.headers}"
        assert response.statusCode == HttpStatus.OK
        Document doc = Jsoup.parse(response.body)
        MultiValueMap<String, Object> formPostParameters = new LinkedMultiValueMap<String, Object>()
        doc.select('input[type="hidden"]').each {
            formPostParameters.add(it.attr('name'), it.val())
        }
        formPostParameters.add("username", "test")
        formPostParameters.add("password", "test")
        println "Using formPostParameters = $formPostParameters"

        and: "post auth"
        headers = new HttpHeaders().with {
            setContentType(MediaType.APPLICATION_FORM_URLENCODED)
            setAccept([MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML])
            add(HttpHeaders.HOST, "${this.casBaseUrl.host}:${this.casBaseUrl.port}")
            add(HttpHeaders.REFERER, loginUrl)
            it
        }
        println "Posting with headers $headers"
        response = restTemplate.exchange(new RequestEntity<MultiValueMap<String, Object>>(formPostParameters, headers, HttpMethod.POST, new URI(loginUrl)), String)
        println "postAuth statusCode=${response.statusCode}, headers=${response.headers}"
        assert response.statusCode == HttpStatus.FOUND
        def endpointUrl = response.getHeaders().get(HttpHeaders.LOCATION).first()
        assert endpointUrl.startsWith("${baseUrl}/protected?ticket=")

        and: "validate ticket at endpoint"
        headers = new HttpHeaders().with {
            setAccept([MediaType.APPLICATION_JSON])
            add(HttpHeaders.HOST, "${this.baseUrl.host}:${this.baseUrl.port}")
            add(HttpHeaders.REFERER, loginUrl)
            it
        }
        response = restTemplate.exchange(new RequestEntity<Object>(headers, HttpMethod.GET, new URI(endpointUrl)), String)
        println "validateTicket statusCode=${response.statusCode}, headers=${response.headers}"
        assert response.statusCode == HttpStatus.FOUND
        endpointUrl = response.getHeaders().get(HttpHeaders.LOCATION).first()
        assert endpointUrl.startsWith("${baseUrl}/protected")
        def validateCookieValues = response.getHeaders().get(HttpHeaders.SET_COOKIE)
        def validateCookies = validateCookieValues.collect {
            HttpCookie.parse(it).first()
        }

        and: "retrieve endpoint content"
        headers = new HttpHeaders().with {
            setAccept([MediaType.APPLICATION_JSON])
            add(HttpHeaders.HOST, "${this.baseUrl.host}:${this.baseUrl.port}")
            it
        }
        validateCookies.each {
            headers.add(HttpHeaders.COOKIE, "${it.name}=${it.value}")
        }
        println "Retrieving using headers $headers"
        response = restTemplate.exchange(new RequestEntity<Object>(headers, HttpMethod.GET, new URI(endpointUrl)), Map<String, Object>)
        println "getEndpoint statusCode=${response.statusCode}, headers=${response.headers}, body=${response.body}"

        then:
        response.statusCode == HttpStatus.OK
        response.body.principal
        with(response.body.principal as Map<String, Object>) {
            name == "1"
            with(attributes as Map<String, Object>) {
                cn == "test"
                givenName == "Test"
                sn == "Person"
                uid == "1"
            }
        }
    }
}
