Run
===

Set up a local PostgreSQL instance. For MacOS, an easy way is to use <http://postgresapp.com/>. Connect to it and run,

```postgresql
CREATE DATABASE mool_conversion;
```

The locations for the mool repo and converted repo are hard-coded.

mool repo: `~/git/data/vostok`

converted repo: `/tmp/mool-conversion`

There are three main classes.

* com.rocketfuel.build.db.MainDeploy creates the tables and views to hold the mool repository and views for the converted project. It then reads the mool repository and writes it to the database.
* com.rocketfuel.build.db.MainConvert reads the views that represent the Maven repository, creates POM files, and copies source files.
* com.rocketfuel.build.db.MainPoms reads the Maven models from the database and only creates POM files.

Development
===========

Run `MainDeploy`. Using a database client of your choice, connect to the `mool_conversion` database. Create functions and views to your heart's content. When you are happy, you can add them to part of the deployment process. You can see the existing ones in [mvn](src/main/resources/com/rocketfuel/build/db/mvn) and [Deploy.scala](src/main/scala/com/rocketfuel/build/db/Deploy.scala). If you are adding a new target project type, you should create a new schema in the database, along with a sibling package to the `mvn` package, e.g. `com.rocketfuel.build.db.gradle`.

Import Converted Project in IntelliJ
====================================

The initial import requires a lot of memory and time. Expect about 15 minutes. After the initial import, pom changes import quickly (a few seconds). If you ask to have sources downloaded, that can take much longer.

In IntelliJ settings, Build, Execution, Deployment/Build Tools/Maven/Importing, set `VM options for importer` to `-Xmx4g`.

It also helps to increase IntelliJ's memory. `Help/Edit Custom VM Options...` Replace `-Xmx2g` with `-Xmx6g`.
