# How to Build the System

## The most basic way

`./stop.sh; ./cyrusFetch.sh; ./cyrusBuild.sh; docker volume prune -f; docker system prune -f; ./run.sh dev1 up`

`./stop.sh; ./cyrusClone.sh; ./cyrusBuild.sh; docker volume prune -f; docker system prune -f; ./run.sh dev1 up`

## The Jenkins way
1. Go to your local projects directory and create a new directory 

    `mkdir genny-*todays date*`

2. Move your existing genny folder to the new directory created 

    `mv genny genny-*todays date*`

3. Make another genny directory in projects 

    `mkdir genny`
    
4. Change folders

    `cd genny`
5. Copy your old genny-main into the new genny 

    `cp -rp ../genny-*todays date*/genny/genny-main .`
6. Change folders
    `cd genny-main`
7. Double check if `SKIP_GOOGLE_DOC_IN_STARTUP=FALSE` is set in `docker-compose.yml` in genny-main

8. Run this script in genny-main inside the new genny folder 
    
    `./cyrusClone.sh;./cyrusBuild.sh;docker volume prune -f;./run.sh dev1 up`

9. Now you wait, this will take from about 20 - 45 mins. If you get lucky might be below 20 mins.

