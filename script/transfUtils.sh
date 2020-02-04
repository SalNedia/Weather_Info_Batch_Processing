#!/bin/bash

address=${1?Error: no address given}
path=${2?Error: no path given}
pemKEy=${3?Error: no key given}
Jarpath1=${4?Error: no path given}
Jarpath2=${4?Error: no path given}
Jarpath3=${4?Error: no path given}





scp -i $pemKEy $path/data/pressure.csv   		      $address:
scp -i $pemKEy $path/data/humidity.csv  			  $address:
scp -i $pemKEy $path/data/temperature.csv	  		  $address:
scp -i $pemKEy $path/data/city_attributes.csv	  	  $address:
scp -i $pemKEy $path/data/weather_description.csv	  $address:

scp -i  $pemKEy $path/                                $address:

scp -i sabdKey.pem $Jarpath1                          $address:
scp -i sabdKey.pem $Jarpath2                          $address:
scp -i sabdKey.pem $Jarpath3                          $address:




