# GAIA :: dataset-exporter

A simple application that exports data from the Spark Works infrastructure and the GAIA store and formats them as a packaged dataset.
The dataset is then stored on the local machine in a set of csv files under a folder generated for this specific export job.
The format of the folder is the following:

````
buildinsXYZ
  |_ description.txt
  |_ temperature.csv
  |_ humidity.csv
  |_ luminosity.csv
  |_ occupancy.csv
  |_ current.csv
  |_ power.csv
  |_ floor1
  | |_ temperature.csv
  | |_ humidity.csv
  | |_ luminosity.csv
  | |_ occupancy.csv
  | |_ current.csv
  | |_ power.csv
  | |_ room1
  | | |_ temperature.csv
  | | |_ humidity.csv
  | | |_ luminosity.csv
  | | |_ occupancy.csv
  | | |_ current.csv
  | | |_ power.csv
  | |_ room2
  | | |_ temperature.csv
  | | |_ humidity.csv
  | | |_ luminosity.csv
  | | |_ occupancy.csv
  | | |_ current.csv
  | | |_ power.csv
  |_ floor2  
    |_ room3
    | | |_ temperature.csv
    | | |_ humidity.csv
    | | |_ luminosity.csv
    | | |_ occupancy.csv
    | | |_ current.csv
    | | |_ power.csv
    |_ room4
        |_ temperature.csv
        |_ humidity.csv
        |_ luminosity.csv
        |_ occupancy.csv
        |_ current.csv
        |_ power.csv

````

##  description.txt
The description.txt contains some background information on the building itself including the units of measurement for the sensors, floor levels, the orientation and the usage of the rooms.
A description.txt would look like the following:

```
Building: 195
current: mA
power: mWh
temperature: C
humidity: %RH
luminosity: lux
occupancy: 0 empty, 1 full, 0.xx intermediate
Floor: 214
         level: 0
Room: 197
         facing: N 
         usage: classroom
Room: 203
         facing: S
         usage: classroom
Floor: 216
         level: 1 
Floor: 215
         level: -1
Room: 200
         facing: W
         usage: lab
Room: 199
         facing: W 
         usage: gym
``` 

## csv files
The csv files contain the following columns:
+ ``Epoch`` the Unix time of the measurement
+ ``Year`` the Year of the measurement
+ ``Month`` the Month of the measurement
+ ``Day`` the Day of the measurement
+ ``Hour`` the Hour of the measurement
+ ``Minute`` the Minute of the measurement
+ ``X`` the actual measurement for this interval


## exporting data

To export a dataset from the online database of the schools you need run the generated jar file using the following arguments:
+ `sso.clientId` your application's clientId in the SparkWorks platform
+ `sso.clientSecret` your application's clientSecret in the SparkWorks platform
+ `sso.username` your GAIA username 
+ `sso.password` your GAIA password
+ `export.uuids` the uuids of the schools you are interested in, in a comma separated list
+ `export.from` the epoch time to start the dataset from
+ `export.to` the epoch time to end the dataset to

The full command would be the following

```
java -jar dataset-exporter-1.0-SNAPSHOT.jar --sso.clientId={clientId} --sso.clientSecret={clientSecret}  --sso.username={username} --sso.password={password} --export.uuids={uuid1},{uuid2}
```