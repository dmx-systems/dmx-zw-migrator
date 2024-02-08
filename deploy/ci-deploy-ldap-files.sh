#!/bin/bash

if [ -f deploy/ldap/ldap-cfg.tar.gz ]; then
    if [ -d deploy/ldap/ldap-cfg ]; then
        rm -rf deploy/ldap/ldap-cfg
    fi
    tar -xzvf deploy/ldap/ldap-cfg.tar.gz -C deploy/ldap/
    rm deploy/ldap/ldap-cfg.tar.gz
fi

if [ -f deploy/ldap/ldap-db.tar.gz ]; then
    if [ -d deploy/ldap/ldap-db ]; then
        rm -rf deploy/ldap/ldap-db
    fi
    tar -xzvf deploy/ldap/ldap-db.tar.gz -C deploy/ldap/
    rm deploy/ldap/ldap-db.tar.gz
fi
