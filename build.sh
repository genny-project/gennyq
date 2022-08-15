#!/bin/bash

if [ "$#" -ge 1 ]; then
	projects=( gennyq $@ )
else
	./mvnw clean install -Dcheckstyle.skip -Dstyle.color=always
	if [[ "$?" -ne 0 ]] ; then
		echo "Build failed"
		exit 1;
	else
		exit 0;
	fi
fi

# iterate projects
for project in "${projects[@]}"
do
    echo "Building $project"

	# perform clean install
	./mvnw clean install -Dcheckstyle.skip -Dstyle.color=always -pl :$project

	# exit if build failed
	if [[ "$?" -ne 0 ]] ; then
		echo "Build failed"
		exit 1;
	fi
done

