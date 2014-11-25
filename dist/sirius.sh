#!/bin/bash
##############################################################################
# Made with all the love in the world
# by scireum in Remshalden, Germany
#
# Copyright by scireum GmbH
# http://www.scireum.de - info@scireum.de
##############################################################################
#
# Start / Stop script for SIRIUS applications
#
# Can be used to start or stop sirius based applications. This is compatible
# with SYSTEM V init.d
#
# A custom configuration can be provided via config.sh as this file should
# not be modified, since it's part of the release.
#
##############################################################################

echo "SIRIUS Launch Utility"
echo "====================="
echo ""

# Contains the java command to execute in order to start the system.
# By default, we assume that java is present in the PATH and can therefore
# be directly started.
JAVA_CMD="java"
CMD_SUFFIX=""

# Shutdown port used to signal the application to shut down. Used different
# ports for different apps or disaster will strike !
SHUTDOWN_PORT="9191"

# File used to pipe all stdout and stderr output to
STDOUT="logs/stdout.txt"

# Enable authbind so that apps can use ports < 1024
# To enabled port 80:
# cd /etc/authbind/byport
# touch 80
# chown USER:USER
# chmod 700 80
LD_PRELOAD="/usr/lib/authbind/libauthbind.so.1"

if [ -f config.sh ]
then
	echo "Loading config.sh..."
	source config.sh
else
	echo "Use a custom config.sh to override the settings listed below"
fi

if [ -z "$SIRIUS_HOME" ]; then
    SIRIUS_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
fi

if [ -z "$JAVA_XMX" ]; then
    JAVA_XMX="1024m"
fi

if [ -z "$JAVA_OPTS" ]; then
    JAVA_OPTS="-server -Xmx$JAVA_XMX -Djava.net.preferIPv4Stack=true"
fi

echo ""
echo "SIRIUS_HOME    $SIRIUS_HOME"
echo "JAVA_CMD:      $JAVA_CMD"
echo "JAVA_OPTS:     $JAVA_OPTS"
echo "SHUTDOWN_PORT: $SHUTDOWN_PORT"
echo "STDOUT:        $STDOUT"
echo "USER_ID:       $USER_ID"
echo ""

case "$1" in
start)
    cd $SIRIUS_HOME
	if [ -f $STDOUT ] 
	then 
		rm $STDOUT 
	fi
	echo "Starting Application..."
   	$JAVA_CMD $JAVA_OPTS IPL >> $STDOUT $CMD_SUFFIX &
    ;;

stop) 
    cd $SIRIUS_HOME
    echo "Stopping Application..."
	$JAVA_CMD -Dkill=true -Dport=$SHUTDOWN_PORT IPL
    ;;

restart)
    cd $SIRIUS_HOME
    echo "Stopping Application..."
    java -Dkill=true -Dport=$SHUTDOWN_PORT IPL
	if [ -f $STDOUT ] 
	then 
		rm $STDOUT 
	fi
    echo "Starting Application..."
    $JAVA_CMD $JAVA_OPTS IPL >> $STDOUT $CMD_SUFFIX &
	;;

patch)
    cd $SIRIUS_HOME
    echo "Stopping Application..."
    $JAVA_CMD -Dkill=true -Dport=$SHUTDOWN_PORT IPL

    sds pull

	if [ -f $STDOUT ]
	then
		rm $STDOUT
	fi
	echo "Starting Application..."
   	$JAVA_CMD $JAVA_OPTS IPL >> $STDOUT $CMD_SUFFIX &
    ;;

*)
    echo "Usage: sirius.sh start|stop|restart|patch"
    exit 1
    ;;

esac
