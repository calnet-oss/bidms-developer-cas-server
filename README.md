More or less a stock [CAS](http://www.github.com/apereo/cas/) server for the
purposes of Berkeley Identity Management Suite (BIDMS) development and/or
testing.  Not meant for production.

Create a CAS configuration file:
```
cp src/main/resources/cas_config.yml.template src/main/resources/cas_config.yml
```

And then you'll need to edit this file.  It doesn't have to live in
`src/main/resources/cas_config.yml` if you're building a WAR file.  When
deploying the WAR file to an application server, you'll need to start up the
application server with `-Dcas.standalone.config.file=PATH/cas_config.yml`,
pointing to the location of your CAS configuration file.  You'll probably
also have to set `-Dcas.standalone.config=PATH` where PATH is the directory
to [src/main/resources/application.yml](application.yml).

You'll need to generate a keypair for TLS.  You can use the
[genKey.sh](genKey.sh) script.

You can run as `./gradlew bootRun --console plain` or build a WAR file with
`./gradlew war`.  

Any customization files here are licensed under the [BSD
two-clause license](LICENSE.txt) ([CAS itself is
licensed](https://github.com/apereo/cas/blob/master/LICENSE) differently).
