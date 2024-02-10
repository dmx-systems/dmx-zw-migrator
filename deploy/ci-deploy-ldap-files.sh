#!/bin/bash

if [ -f deploy/ldap/ldap-cfg.tar.gz ]; then
    if [ -d deploy/ldap/ldap-cfg ]; then
        rm -rf deploy/ldap/ldap-cfg
    fi
    mkdir deploy/ldap/ldap-cfg
    # tar -xzvf deploy/ldap/ldap-cfg.tar.gz -C deploy/ldap/
    #rm deploy/ldap/ldap-cfg.tar.gz
fi

if [ -f deploy/ldap/ldap-db.tar.gz ]; then
    if [ -d deploy/ldap/ldap-db ]; then
        rm -rf deploy/ldap/ldap-db
    fi
    mkdir deploy/ldap/ldap-db
    #tar -xzvf deploy/ldap/ldap-db.tar.gz -C deploy/ldap/
    #rm deploy/ldap/ldap-db.tar.gz
fi

#chmod 600 deploy/ldap/certs/dhparam.pem
#chmod 644 deploy/ldap/certs/ldap.crt
#chmod 600 deploy/ldap/certs/ldap.key
#chmod 664 deploy/ldap/certs/ca.crt

