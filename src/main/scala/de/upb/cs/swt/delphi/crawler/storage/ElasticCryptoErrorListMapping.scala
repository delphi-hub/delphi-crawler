// Copyright (C) 2018 The Delphi Team.
// See the LICENCE file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.upb.cs.swt.delphi.crawler.storage

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldDefinition

object ElasticCryptoErrorListMapping {

  //Returns a Seq object of FieldDefinitions that defines all fields in the feature mapping
  def getMapAsSeq: Seq[FieldDefinition] =
    cryptoErrorMap.toSeq.map{case (name, fun) => fun(name)}

  //Stores the mapping for the error type and count returned by CryptoAnalysis as a Map of error names to field types

  private val cryptoErrorMap: Map[String, String => FieldDefinition] = Map[String, String => FieldDefinition](
    "AbstractError" -> intField,
    "ConstraintError" -> intField,
    "ErrorVisitor" -> intField,
    "ErrorWithObjectAllocation" -> intField,
    "ForbiddenMethodError" -> intField,
    "HardCodedError" -> intField,
    "IError" -> intField,
    "ImpreciseValueExtractionError" -> intField,
    "IncompleteOperationError" -> intField,
    "InstanceOfError" -> intField,
    "NeverTypeOfError" -> intField,
    "PredicateContradictionError" -> intField,
    "RequiredPredicateError" -> intField,
    "TypestateError" -> intField)
}
