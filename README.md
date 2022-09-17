# Rapidomize SDK for java
Rapidomize Client Library for Java (https://rapidomize.com)

Supports HTTP, WebSocket & MQTT connectivity with TLS/SSL.

You'll need to decide on a way to interact with server/cloud platform: either using the SDK or the raw protocols.
Its may be convenient to use sdk for handling authentication and authorization, so the SDK is the recommended
method for connecting to the server/cloud platform.

# Using the SDK

The recommended way to use the SDK in your project is to consume it from Maven Central.

```xml
<dependency>
    <groupId>com.rapidomize.ics.sdk</groupId>
    <artifactId>rpzc</artifactId>
    <version>0.7.5</version>
</dependency>
```

*or if you're using Gradle:*

```
compile 'com.rapidomize.ics.sdk:rpzc:0.7.5'
```

# Building From Source
You clone this repo and build the SDK artifact (jar) by yourself.

```
$ mvn clean install
```

# Status
version 0.7.5 - 'Dugong Weasel'

# Contributions?
Contributions are highly welcome. If you're interested in contributing please leave a note with your username.

# Policy for Security Disclosures
If you suspect you have uncovered a vulnerability, contact us privately, as outlined in our security policy document; we will immediately prioritize your disclosure.

# License

Apache 2.0

