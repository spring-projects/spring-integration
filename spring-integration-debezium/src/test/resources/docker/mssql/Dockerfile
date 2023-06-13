FROM mcr.microsoft.com/mssql/server:2022-latest
ENV ACCEPT_EULA y
ENV MSSQL_SA_PASSWORD MyFancyPassword123
ENV MSSQL_PID Standard
ENV MSSQL_AGENT_ENABLED true
# COPY ./init.sql .
COPY ./init.sql ./init.sql
COPY ./entrypoint.sh .
EXPOSE 1433
CMD /bin/bash ./entrypoint.sh