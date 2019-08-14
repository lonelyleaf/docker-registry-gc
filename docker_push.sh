#!/bin/bash

if [ $TRAVIS_TAG ]; then
  image="lonelyleaf/docker-registry-gc:$TRAVIS_TAG"
else
  image="lonelyleaf/docker-registry-gc"
fi

echo "start deploy $image"
docker build -t "$image"
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
docker push "$image"
