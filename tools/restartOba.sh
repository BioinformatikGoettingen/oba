#! /bin/bash

LOGFILE="/tmp/obarestart.log"

restart_oba(){
    fuser -k 9998/tcp >> $LOGFILE    
    #jar=$(ls /root/.hudson/jobs/oba/workspace/oba/oba-server/target/oba-server*jar-with-dependencies.jar)
    cp /tmp/oba-server*jar-with-dependencies.jar /var/tmp/oba-server.jar
    #screen -S oba -D -m java -Xmx750m -cp /var/tmp/oba-server.jar de.sybig.oba.server.RestServer >> $LOGFILE  &
    java -Xmx750m -cp /var/tmp/oba-server.jar de.sybig.oba.server.RestServer >> $LOGFILE  &
}

echo "script started" > $LOGFILE

while true
do
    if [ /tmp/oba-server*jar-with-dependencies.jar -nt /var/tmp/oba-server.jar ]
    then
	restart_oba
    fi
    sleep 300
done
echo "exit" >> $LOGFILE

