# Common Alerting Protocol Library [![Build Status](https://travis-ci.org/google/cap-library.svg?branch=master)](https://travis-ci.org/google/cap-library)
Copyright 2014 Google Inc.

http://github.com/google/cap-library/

===========

The CAP Library is a collection of code and tools to work with public alerting
messages in the [Common Alerting Protocol]
(http://en.wikipedia.org/wiki/Common_Alerting_Protocol) format.

Namely, a well-tested and easy-to-use Java library that supports
* creation and parsing of feeds in the CAP format,
* validating of feeds against common CAP profiles.

Moreover, it includes a simple [web application]
(http://cap-validator.appspot.com/) to validate the correctness of CAP messages.

## About

The CAP Library is designed to support CAP versions 1.0, 1.1, and 1.2.
There are classes that can parse XML CAP messages as well as easily
create new messages and write them to XML, JSON, (soon) ASN.1, and (soon) KML.

The main data structures are auto-generated from a Google protocol
buffer implementation of the CAP spec in proto/cap.proto. Protocol buffers 
are Google's language-neutral, platform-neutral, extensible mechanism for
serializing structured data - think XML, but smaller, faster, and simpler.

The generated classes offer a clean API for creating and manipulating
alert objects. The alert data structures are immutable; they provide only
getters.  New alerts are constructed via Builder classes. See 
`javatests/com/google/publicalerts/cap/TestUtil.java`, for an example.

`javatests/com/google/publicalerts/cap/EndToEndTest.java` provides a good
overview of how to get started using the library.

To learn more about Google protocol buffers, see
http://code.google.com/apis/protocolbuffers/ and http://code.google.com/apis/protocolbuffers/docs/reference/java-generated.html


## Development

###Install ant
You can download and install Apache Ant from http://ant.apache.org/.

###Run all tests
```
» APPENGINE_JAVA_SDK=/path/to/appengine-sdk ant test
```

###Compile the library into a jar
```
» cd java
» ant jar
```

###Compile and run the validator

```
» cd validator
» APPENGINE_JAVA_SDK=/path/to/appengine-sdk ant runserver
```
