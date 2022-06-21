#!/bin/sh

#
# Usage: genKey.sh [CAS_CONFIG_DIR]
#
# If CAS_CONFIG_DIR is not specified, then it defaults to the test-cas
# directory.
# The DN environment variable can also be set to override the default
# certificate DN.
#

if [ -z "$CAS_CONFIG_DIR" ]; then
  if [ ! -z "$1" ]; then
    CAS_CONFIG_DIR="$1"
  else
    CAS_CONFIG_DIR="test-cas"
  fi
fi

if [ -z "$DN" ]; then
  DN="cn=localhost,ou=bidms-developer-cas-server,o=BIDMS,st=California,c=US"
fi

if [ ! -e "$CAS_CONFIG_DIR" ]; then
  echo "$CAS_CONFIG_DIR does not exist yet." >&2
  exit 1
fi

keytool -genkey -keyalg RSA -keysize 4096 -keystore ${CAS_CONFIG_DIR}/testkeystore.jks -dname "$DN" -alias mykey -storepass changeit -keypass changeit -validity 10000 \
  && chmod 600 ${CAS_CONFIG_DIR}/testkeystore.jks \
  && keytool -exportcert -keystore ${CAS_CONFIG_DIR}/testkeystore.jks -storepass changeit -alias mykey -rfc -file ${CAS_CONFIG_DIR}/pubkey.pem \
  && keytool -importcert -keystore ${CAS_CONFIG_DIR}/testtruststore.jks -storepass changeit -alias pubkey -trustcacerts -noprompt -file ${CAS_CONFIG_DIR}/pubkey.pem
