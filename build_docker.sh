#!/bin/bash
wget -c https://github.com/LSTS/ImcService/releases/download/1.0-SNAPSHOT/dune-2020.01.0.tar.bz2
docker build . -t dune:2020.1.0
