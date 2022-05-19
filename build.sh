#!/bin/bash

if [ "$#" -ge 1 ]; then
	projects=( gennyq $@ )
else
	projects=( gennyq qwandaq serviceq gadaq bridge fyodor dropkick lauchy messages shleemy )
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

	# exit if build failed
	if [[ "$?" -ne 0 ]] ; then
		echo "Build failed"
		exit $rc
	fi
done

