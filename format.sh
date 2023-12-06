#!/usr/bin/env bash

for f in api gateway authorization; do
  echo "the directory is $f"
  cd $f
  ./mvnw -f pom.xml spring-javaformat:apply
  cd ..
done

cd studio
npm run format