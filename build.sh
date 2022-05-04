#!/bin/bash

if [ "$#" -ge 1 ]; then
	projects=( $@ )
else
	projects=( qwandaq serviceq gadaq bridge fyodor dropkick lauchy messages shleemy )
fi

for project in "${projects[@]}"
do
    echo "Building $project"
	./mvnw clean install -DskipTests=true -Dcheckstyle.skip -Dstyle.color=always -pl :$project
done

