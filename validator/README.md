# Common Alerting Protocol Validator
Copyright 2014 Google Inc.

http://github.com/google/cap-library/

===========

## Usage
The Common Alerting Protocol validator is a simple web application that uses
the CAP library to check the syntax of CAP XML messages and Atom and RSS feeds
of CAP messages. It supports CAP v1.0, v1.1 and v1.2.

### Features

- Supports URLs to CAP messages and feeds of CAP as well as direct entry of the
  messages or feeds.
- Supports validating against common CAP profiles (ex. US IPAWS and Canada
  CANALERT).
- Renders errors and recommendations in-line for easy identification of problems.
- Displays valid alerts using the Google Maps API for easy visualization of
  circles and polygons.

## Development

###Install ant
You can download and install Apache Ant from http://ant.apache.org/.

###Run all tests
```
» APPENGINE_JAVA_SDK=/path/to/appengine-sdk ant test
```

###Run the validator
The validator can run under any standard servlet container (Tomcat, Jetty,
Google App Engine, etc). An ant target is supplied for starting the 
dev App Engine server. You can download the App Engine SDK at 
http://code.google.com/appengine/downloads.html#Google_App_Engine_SDK_for_Java

Then run
```
» APPENGINE_JAVA_SDK=/path/to/your/appengine_java_sdk ant runserver
```

Finally, visit the following URL in your browser: `http://localhost:8090`
