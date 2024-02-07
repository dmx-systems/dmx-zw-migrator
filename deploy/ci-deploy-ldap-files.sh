#!/bin/bash

if [ -f deploy/ldap/ldap-cfg.tar.gz ]; then
    tar -xzvf deploy/ldap/ldap-cfg.tar.gz -C deploy/ldap/
    rm deploy/ldap/ldap-cfg.tar.gz
fi

if [ -f deploy/ldap/ldap-db.tar.gz ]; then
    tar -xzvf deploy/ldap/ldap-db.tar.gz -C deploy/ldap/
    rm deploy/ldap/ldap-db.tar.gz
fi
