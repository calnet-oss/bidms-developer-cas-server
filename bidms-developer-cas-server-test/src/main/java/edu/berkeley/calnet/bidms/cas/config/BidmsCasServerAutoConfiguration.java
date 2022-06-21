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
package edu.berkeley.calnet.bidms.cas.config;

import io.github.bkoehm.apacheds.embedded.EmbeddedLdapServer;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.api.CoreSession;
import org.apereo.cas.config.CasPropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = CasPropertiesConfiguration.class)
public class BidmsCasServerAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(BidmsCasServerAutoConfiguration.class);

    @Bean(destroyMethod = "destroy")
    EmbeddedLdapServer getEmbeddedLdapServer() throws Exception {
        EmbeddedLdapServer embeddedLdapServer = new EmbeddedLdapServer();
        embeddedLdapServer.init();
        // username is test, password is test
        // can be confirmed with: ldapsearch -x -w test -D cn=test,dc=mydomain,dc=org -b cn=test,dc=mydomain,dc=org -LLL -H ldap://localhost:10389 "(objectClass=*)"
        createUserEntry(embeddedLdapServer, "1", "test", "test", "Test", "Person");
        return embeddedLdapServer;
    }

    private static void createUserEntry(EmbeddedLdapServer embeddedLdapServer, String uid, String cn, String password, String givenName, String sn) throws LdapException {
        Entry entry = embeddedLdapServer.getDirectoryService().newEntry(
                embeddedLdapServer.getDirectoryService().getDnFactory().create("cn=" + cn + "," + embeddedLdapServer.getBaseStructure())
        );
        entry.add("objectClass", "person", "inetOrgPerson");
        entry.add("uid", uid);
        entry.add("cn", cn);
        entry.add("userPassword", password);
        entry.add("givenName", givenName);
        entry.add("sn", sn);
        CoreSession session = embeddedLdapServer.getDirectoryService().getAdminSession();
        try {
            session.add(entry);
        } finally {
            session.unbind();
        }
    }
}
