#!/bin/bash

#address=52.59.34.190
address=${1?Error: no address given}
key=${2?Error: no key given}
typeFile=${3?Error: no typefile specified}



ssh -i $key hadoop@$address

mkdir data
mkdir avs
mkdir avro
mkdir parquet

mv pressure.csv    data
mv humidity.csv    data
mv temperature.csv data
mv weather_description.csv data
mv city_attributes.csv data






