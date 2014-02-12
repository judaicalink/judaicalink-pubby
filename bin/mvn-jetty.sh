#!/bin/bash
mvn compile jetty:run -Djava.util.logging.config.file=src/main/resources/jetty-logging.properties
