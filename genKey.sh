#!/bin/sh

DN="cn=HOSTHERE,ou=CalNet - IST - CAS Development,o=University of California\, Berkeley,st=California,c=US"
keytool -genkey -keyalg RSA -keysize 8192 -keystore src/main/resources/testkeystore.jks -dname "$DN" -alias mykey -storepass changeit -keypass changeit
chmod 600 src/main/resources/testkeystore.jks
