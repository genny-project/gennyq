#!/bin/bash

if [ "$#" -ge 1 ]; then
	projects=( $@ )
else
	projects=( qwandaq serviceq gadaq bridge fyodor dropkick lauchy messages shleemy )
fi

# iterate projects
for project in "${projects[@]}"
do
    echo "Building $project"

	# remove target dir
	rm -rf $project/target
	rm -rf kogito/$project/target

	# perform clean install
	./mvnw clean install -DskipTests=true -Dcheckstyle.skip -Dstyle.color=always -pl :$project
done

