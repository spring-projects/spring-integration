#!/bin/bash

# Start SQL server
(/opt/mssql/bin/sqlservr &) ; sleep 10  ; echo "Run init.sql ..." ;  /opt/mssql-tools/bin/sqlcmd -S localhost -U SA -P $MSSQL_SA_PASSWORD -i init.sql

# Block the container from exiting.
echo "Wait indefinitely ..."
while true; do sleep 86400; done
