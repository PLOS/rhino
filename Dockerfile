FROM tomcat:7.0-jre8-alpine
RUN apk add gettext # for envsubst
WORKDIR $CATALINA_HOME
ARG WAR_FILE
COPY with-templates.sh /bin/with-templates.sh
RUN chmod a+x /bin/with-templates.sh
ENTRYPOINT ["/bin/with-templates.sh"]
# Setting ENTRYPOINT resets CMD
CMD ["catalina.sh", "run"]
RUN wget http://central.maven.org/maven2/mysql/mysql-connector-java/5.1.28/mysql-connector-java-5.1.28.jar -O/usr/local/tomcat/lib/mysql-connector-java-5.1.28.jar

ENV JAVA_OPTS -Drhino.configDir=/rhino/conf/
RUN mkdir -p /rhino/conf
COPY target/$WAR_FILE /usr/local/tomcat/webapps/v2.war
COPY src/deb/tomcat7/conf/context.template.xml /usr/local/tomcat/conf/context.template.xml
COPY rhino.template.yaml /rhino/conf/rhino.template.yaml
ENV TEMPLATES "/rhino/conf/rhino.template.yaml /usr/local/tomcat/conf/context.template.xml"
