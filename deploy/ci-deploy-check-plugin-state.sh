#!/bin/bash

#  check-plugin-state.sh
#
#  A shell script that waits until a DMX plugin is fully started by parsing the logfile.
#
#  Copyright 2024, DMX Systems <https://dmx.systems>
#  Authored by Juergen Neumann <juergen@dmx.systems>
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation; either version 3 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program; if not, write to the Free Software
#  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
#  MA 02110-1301, USA.
#
#


function check_state() {
    LOGFILE="$1"
    NUMREGEX='^[0-9]+$'
    if [[ -n "$2" ]] && [[ "$2" =~ ${NUMREGEX} ]]; then
        WAITSTART="$2"
    else
        WAITSTART=180
    fi
    ## grep what we are we looking for
    STARTS="$( cat ${LOGFILE} | grep -c "INFO: ### Bundles total: 46, DMX plugins: 24, Activated: 24" )"
    if [ -z "${STARTS}" ]; then
        STARTS = 0
    fi
    OLDERRORS="$( cat ${LOGFILE} | grep -c "SEVERE: " )"
    GOAL=$(( ${STARTS} + 1 ))
    ERRORS=0
    COUNT=0
    ## check if dmx started without errors
    echo "INFO: Scanning ${LOGFILE} for message about successful start (max. ${WAITSTART}s)."
    while [ "${STARTS}" -lt "${GOAL}" ] && [ "${COUNT}" -lt "${WAITSTART}" ]; do
    sleep 1
    COUNT=$(( ${COUNT} + 1 ))
    STARTS="$( cat ${LOGFILE} | grep -c "INFO: DMX plugin started in" )"
    ERRORS="$( cat ${LOGFILE} | grep -c "SEVERE: " )"
    ERRORS=$(( ${ERRORS} - ${OLDERRORS} ))
    if [ ${ERRORS} -gt 0 ]; then
        ERRORMSG="$( cat ${LOGFILE} | grep -A1 "SEVERE:" | tail -n1 )"
        echo "WARNING! Found ${ERRORS} errors. Last error message:"
        echo "${ERRORMSG}"
            exit 1
    fi
    done

    ## debug
    #echo "DEBUG:  STARTS: ${STARTS}, GOAL: ${GOAL}, COUNT: ${COUNT}, WAITSTART: ${WAITSTART}"

    ## report status
    if [ "${STARTS}" -lt "${GOAL}" ]; then
    echo "ERROR! Failed to start DMX in ${WAITSTART} seconds."
    exit 1
    else
    STARTTIME="$( cat ${LOGFILE} | grep "INFO: DMX platform started in" | tail -n 1 )"
    echo "${STARTTIME} (COUNT=${COUNT})"
    echo "INFO: Success."
    fi
}

## RUN
echo "$1" "$2"
if [ -f "$1" ]; then
    echo "INFO: Found logfile $1."
    check_state "$1" "$2"
elif [ -z "$1" ]; then
    echo "ERROR! Missing path to DMX logfile."
    exit 1
else
    ## Wait for DMX to create logfile
    COUNT=0
    WAITFILE=90
    echo "INFO: Waiting for DMX to create logfile $1. (max. ${WAITFILE}s)"
    while [ ! -f "$1" ] && [ ${COUNT} -le ${WAITFILE} ]; do
    sleep 1
    COUNT=$(( ${COUNT} + 1 ))
    done
    if [ -f "$1" ]; then
    echo "INFO: Scanning logfile $1."
    check_state "$1" "$2"
    else
    echo "ERROR! DMX logfile $1 not found after ${COUNT} seconds."
    exit 1
    fi
fi

## EOF