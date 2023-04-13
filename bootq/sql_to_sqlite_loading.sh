#!/bin/bash
dbandsqlfileslist=$1

load_file_to_sqlite() {
  dbname=$1
  sqlfile=$2
  echo "Loading $sqlfile into Sqlite db $dbname.sqlite ..."
  dbfilename=../../genny-main/sqlite/volume_mount/db/$dbname.sqlite
  test -f $dbfilename || touch $dbfilename
  sqlite3 $dbfilename < $sqlfile
  echo "Completed loading $sqlfile into Sqlite db $dbname..."
}

if [ $# != 1 ]; then
  echo "Wrong number of arguments supplied. Usage: ${0} Genny:../../genny-main/sqlite/volume_mount/db/Genny.sql,InternMatchData:../../genny-main/sqlite/volume_mount/db/InternMatchData"
  exit 1
fi

echo "Checking for Sqlite installation..."
if ! sqlite3 --version;
then
  echo "sqlite3 is required for this script to run. Please install before you try again. Exiting..."
  exit 1
fi

echo "Found Sqlite. Proceeding..."

echo "Processing the payload $dbandsqlfileslist ..." 
IFS=','
read -ra dbandsqlfilesarr <<< "$dbandsqlfileslist"
for dbandsqlfiles in "${dbandsqlfilesarr[@]}"
do
  echo "Processing the combo $dbandsqlfiles ..."
  IFS=':'
  read -ra dbandsqlarr <<< "$dbandsqlfiles"
  dbname=${dbandsqlarr[0]}
  filename=${dbandsqlarr[1]}
  if [[ -d $filename ]]
  then
    echo "$filename is a directory. Loading all *.sql files in the directory into Sqlite..."
    for file in "$filename"/*
    do
      load_file_to_sqlite $dbname $file
    done
  elif [[ -f $filename ]]
  then
    load_file_to_sqlite $dbname $filename
  fi
  echo "Completed processing the combo $dbandsqlfiles."
done

echo "Load complete."
