#!/bin/bash

function stop() {
  killall -9 java
  killall -9 dune
}

trap stop SIGINT

java -jar /usr/local/ImcServer.jar 6006 8009 &
nohup /usr/local/dune/bin/dune -c lauv-xplore-1 -p Simulation

