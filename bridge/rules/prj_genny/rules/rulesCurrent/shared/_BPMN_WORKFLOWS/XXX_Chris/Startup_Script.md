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

9. Now you wait, this will take from about 45 - 60 mins. If you get lucky might be below 1 hr.

## Alias Stuff

alias _wr='./logs.sh wildfly-rulesservice'

alias _q='./logs.sh qwanda-service'

alias _i='ssh internmatch'

alias _is='ssh internmatch-staging'

alias _g='cd ~/projects/genny/genny-main'

alias _cq='./stop.sh; ./cyrusFetch.sh; ./cyrusBuild.sh; ./run.sh dev1 up'

alias _cf='./stop.sh; ./cyrusFetch.sh; ./cyrusBuild.sh; docker volume prune -f; docker system prune -f; ./run.sh dev1 up'

alias _rw='./restart.sh wildfly-rulesservice'

alias _r='./run.sh dev1 up'

alias _s='./stop.sh'

alias _d='docker ps'

alias _ss='./cyrusStop.sh; ./run.sh dev1 up'

## Preload Database

1. Move this database file (`genny-2019_12_03__12_58_14.bz2`) to your **genny folder** (`cd projects/genny/`)

2. Go to **genny-main** and set `SKIP_GOOGLE_DOC_IN_STARTUP=TRUE`

3. Go to **genny-main/databaseScripts** and run `./mysql_restore.sh genny-2019_12_03__12_58_14.bz2`

4. Go back to **genny-main** and run/build the system (no need to load google docs or rules)