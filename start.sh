#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $DIR/target/universal/wikimap-1.0-SNAPSHOT/

bin/wikimap

