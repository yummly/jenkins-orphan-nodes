#!/bin/bash

# run repl in a docker container. good for developing on a mac

docker run -it -p 4001:4001 \
       -e LEIN_REPL_HOST=0.0.0.0 -e LEIN_REPL_PORT=4001 \
       -e AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID \
       -e AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
       -e AWS_DEFAULT_REGION=$AWS_DEFAULT_REGION \
       --rm -v `pwd`:/work -v $HOME/.m2:/root/.m2 -w /work clojure lein repl
