#!/bin/bash

if [ "$#" -ge 1 ]; then
	projects=( gennyq $@ )
else
	./mvnw clean install -DskipTests=true -Dcheckstyle.skip -Dstyle.color=always
	exit 0;
fi

# iterate projects
for project in "${projects[@]}"
do
    echo "Building $project"

	# perform clean install
	./mvnw clean install -DskipTests=true -Dcheckstyle.skip -Dstyle.color=always -pl :$project

	# exit if build failed
	if [[ "$?" -ne 0 ]] ; then
		echo "Build failed"
		exit $rc
	fi
done


