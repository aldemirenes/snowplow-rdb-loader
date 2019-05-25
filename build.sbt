/*
 * Copyright (c) 2012-2019 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

lazy val global = project
  .in(file("."))
  .aggregate(
    common,
    fs2shredder,
    loader,
    shredder
  )

lazy val common = project.in(file("common"))
  .settings(Seq(
    name := "snowplow-rdb-loader-common"
  ))
  .settings(BuildSettings.buildSettings)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.igluClient,
      Dependencies.igluCoreCirce,
      Dependencies.scalaTracker,
      Dependencies.scalaTrackerEmit,
      Dependencies.circeGeneric,
      Dependencies.circeGenericExtra,
      Dependencies.circeLiteral,
      Dependencies.schemaDdl,
      Dependencies.analyticsSdk
    )
  )

lazy val fs2shredder = project.in(file("fs2shredder"))
  .settings(
    name := "snowplow-fs2-shredder",
    version := "0.1.0"
  )
  .settings(BuildSettings.buildSettings)
  .settings(BuildSettings.assemblySettings)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.decline,
      Dependencies.igluClient,
      Dependencies.igluCoreCirce,
      Dependencies.scalaTracker,
      Dependencies.scalaTrackerEmit,
      Dependencies.catsFree,
      Dependencies.circeGeneric,
      Dependencies.circeGenericExtra,
      Dependencies.circeLiteral,
      Dependencies.fs2,
      Dependencies.schemaDdl,
      Dependencies.analyticsSdk,
      "co.fs2" %% "fs2-io" % "1.0.4",
      Dependencies.nsqClient,

      Dependencies.specs2,
      Dependencies.specs2ScalaCheck,
      Dependencies.scalaCheck
    )
  )
  .dependsOn(common)

lazy val loader = project.in(file("loader"))
  .settings(
    name := "snowplow-rdb-loader",
    version := "0.17.0",
    initialCommands := "import com.snowplowanalytics.snowplow.rdbloader._",
    Compile / mainClass := Some("com.snowplowanalytics.snowplow.rdbloader.Main")
  )
  .settings(BuildSettings.buildSettings)
  .settings(BuildSettings.scalifySettings(shredder / name, shredder / version))
  .settings(BuildSettings.assemblySettings)
  .settings(resolvers ++= Dependencies.resolutionRepos)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.decline,
      Dependencies.igluClient,
      Dependencies.igluCoreCirce,
      Dependencies.scalaTracker,
      Dependencies.scalaTrackerEmit,
      Dependencies.catsFree,
      Dependencies.circeYaml,
      Dependencies.circeGeneric,
      Dependencies.circeGenericExtra,
      Dependencies.circeLiteral,
      Dependencies.manifest,
      Dependencies.fs2,
      Dependencies.schemaDdl,

      Dependencies.postgres,
      Dependencies.redshift,
      Dependencies.redshiftSdk,
      Dependencies.s3,
      Dependencies.ssm,
      Dependencies.dynamodb,
      Dependencies.jSch,

      Dependencies.specs2,
      Dependencies.specs2ScalaCheck,
      Dependencies.scalaCheck
    )
  )
  .dependsOn(common)

lazy val shredder = project.in(file("shredder"))
  .settings(
    name        := "snowplow-rdb-shredder",
    version     := "0.15.0-rc3",
    description := "Spark job to shred event and context JSONs from Snowplow enriched events",
    BuildSettings.oneJvmPerTestSetting // ensures that only CrossBatchDeduplicationSpec has a DuplicateStorage
  )
  .settings(BuildSettings.buildSettings)
  .settings(resolvers ++= Dependencies.resolutionRepos)
  .settings(BuildSettings.shredderAssemblySettings)
  .settings(BuildSettings.scalifySettings(name, version))
  .settings(BuildSettings.dynamoDbSettings)
  .settings(
    libraryDependencies ++= Seq(
      // Java
      Dependencies.dynamodb,
      // Scala
      Dependencies.decline,
      Dependencies.analyticsSdk,
      Dependencies.eventsManifest,
      Dependencies.circeJawn,
      Dependencies.circeLiteral,
      Dependencies.schemaDdl,
      Dependencies.sparkCore,
      Dependencies.sparkSQL,
      Dependencies.igluClient,
      Dependencies.igluCoreCirce,
      Dependencies.manifest,
      // Scala (test only)
      Dependencies.specs2
    ),

    dependencyOverrides ++= Seq(
      Dependencies.dynamodb,
      "com.fasterxml.jackson.core" % "jackson-core" % "2.6.7",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.7.2"
    )
  )
  .dependsOn(common)
