More or less a stock [CAS](https://www.github.com/apereo/cas/) server for
the purposes of [Berkeley Identity Management Suite
(BIDMS)](https://www.github.com/calnet-oss/bidms) development and/or
testing.  Not meant for production.  Alternatively, there is the [CAS
Initializr service](https://github.com/apereo/cas-initializr).

### Building a WAR file suitable for deployment to an application server

`./gradlew bidms-developer-cas-server-webapp:war`

The resulting WAR file that can be deployed is located at:<br/>
`bidms-developer-cas-server-webapp/build/libs/bidms-developer-cas-server-webapp-${casVersion}-plain.war`

### Configuring to run locally

Decide where your CAS configuration directory is going to be, referred to as
`$CAS_CONFIG_DIR`.  A suggestion is to create a `cas` directory at the
top-level of this project because this how the `bootRun` task is already
configured in [build.gradle](build.gradle).

`mkdir cas`

Place your own `cas_config.yml` file in `$CAS_CONFIG_DIR`.  If you'd like a
sample, you can refer to [test-cas/cas_config.yml](test-cas/cas_config.yml)
which is used for the automated tests.

Next, you'll need a key store.  You're free to create your own, but if you'd
like to get started with a self-signed certificate:
`./genKey.sh $CAS_CONFIG_DIR`

### Running locally with embedded Tomcat

`./gradlew bootRun`

Refer to the [build.gradle](build.gradle) `bootRun` section for boot-up properties.

### Building an executable WAR file with embedded Tomcat

`./gradlew bootWar`

The resulting WAR file that can be executed stand-alone is located at:<br/>
`bidms-developer-cas-server-webapp/build/libs/bidms-developer-cas-server-webapp-${casVersion}.war`

You can use the `JAVA_OPTS` environment variable to set initial CAS
properties similar to what you see in the `bootRun -> jvmArgs` section of
[build.gradle](build.gradle).

### Running the automated tests

A one-time task is to generate a keystore for the `test-cas` directory:<br/>
`./genKey.sh`

Once a keystore is present, tests can be invoked:<br/>
`./gradlew bidms-developer-cas-server-test:test`

These test cases are useful when attempting to upgrade to a newer CAS
version.  Sometimes there are changes to CAS configuration structure.

One notable aspect to these tests is that they attempt to launch the
`test-webapp` (as a bootWar) as a separate process that listens on a
different web port than the CAS server started by the tests.  This test
webapp has an endpoint protected with the [Java CAS
client](https://github.com/apereo/java-cas-client) in order to do full
testing of authentication.  This has been tested in a Linux environment but
further work may be necessary to support running these tests in other
operating systems.

When running the tests, the CAS web server port is `8060` and the
`test-webapp` port is `8070`.

Another notable aspect is that `bidms-developer-cas-server-test` embeds
a LDAP server so that LDAP storage of credentials and attributes can be
tested.  This embedded LDAP server runs on port `10389`.

### License

Any source code provided directly by this project are licensed under the
[BSD two-clause license](LICENSE.txt) ([CAS itself is
licensed](https://github.com/apereo/cas/blob/master/LICENSE) differently).
