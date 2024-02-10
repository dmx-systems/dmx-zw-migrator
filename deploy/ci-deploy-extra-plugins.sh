#!/bin/bash

source deploy/ci-deploy-vars.sh

PLUGINS="$1"
DESTDIR="deploy/instance/${TIER}-ci/bundle-deploy"

if [ -z "$1" ]; then
    echo "ERROR! You must enter a plugin name: e.g. dmx-plugin-xyz."
    exit 1
fi

if [ ! -z "${PLUGINS}" ]; then
    declare -a PLUGINS=(${PLUGINS})
else 
    declare -a PLUGINS=()
fi

echo "INFO: Plugins to install: ${PLUGINS[@]}"

for plugin in ${PLUGINS[@]}; do
    echo "INFO: getting latest version of ${plugin} plugin"
    plugin_version="$( wget -q -O - "${WEBCGI}/ci/${plugin}/${plugin}-latest.jar" )"
    echo "INFO: installing ${plugin_version}"
    wget -q "${plugin_version}" -P "${DESTDIR}"
done
