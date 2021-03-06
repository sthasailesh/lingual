/*
 * Copyright (c) 2007-2013 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.cascading.org/
 *
 * This file is part of the Cascading project.
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

package cascading.lingual.optiq.enumerable;

import java.util.List;

import cascading.tuple.Tuple;
import org.eigenbase.rex.RexLiteral;

/**
 *
 */
public class EnumerableUtil
  {
  static Tuple createTupleFrom( List<RexLiteral> values )
    {
    Tuple tuple = Tuple.size( values.size() );

    for( int i = 0; i < values.size(); i++ )
      tuple.set( i, values.get( i ).getValue2() ); // seem to come out canonical, so bypassing using TupleEntry to set

    return tuple;
    }
  }
