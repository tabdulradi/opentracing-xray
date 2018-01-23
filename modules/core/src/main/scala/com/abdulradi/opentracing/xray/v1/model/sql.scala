/*
 * Copyright 2017 com.abdulradi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.abdulradi.opentracing.xray.v1.model

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uri

final case class Sql(
  /**
    * For SQL Server or other database connections that don't use URL connection strings, record the connection string, excluding passwords.
    */
  connectionString: Option[String],
  /**
    * For a database connection that uses a URL connection string, record the URL, excluding passwords.
    */
  url: Option[String Refined Uri],
  /**
    * The database query, with any user provided values removed or replaced by a placeholder.
    */
  sanitized_query: Option[String],
  /**
    * The name of the database engine.
    */
  database_type: Option[String],
  /**
    * The version number of the database engine.
    */
  database_version: Option[String],
  /**
    * The name and version number of the database engine driver that your application uses.
    */
  driver_version: Option[String],
  /**
    * The database username.
    */
  user: Option[String],
  /**
    * call if the query used a PreparedCall; statement if the query used a PreparedStatement.
    */
  preparation: Option[Preparation]
)

sealed trait Preparation

/**
  * if the query used a PreparedCall
  */
final case object Call extends Preparation

/**
  * if the query used a PreparedStatement.
  */
final case object Statement extends Preparation
