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

package cascading.lingual.flow;

import java.lang.reflect.Type;

import net.hydromatic.linq4j.BaseQueryable;
import net.hydromatic.linq4j.Enumerator;
import net.hydromatic.linq4j.QueryProvider;
import net.hydromatic.linq4j.Queryable;
import net.hydromatic.linq4j.expressions.Expression;

/**
 *
 */
class FlowQueryProvider implements QueryProvider
  {
  /** Creates a QueryProviderImpl. */
  public FlowQueryProvider()
    {
    }

  public <T> Queryable<T> createQuery( Expression expression, Class<T> rowType )
    {
    return new QueryableImpl<T>( this, rowType, expression );
    }

  public <T> Queryable<T> createQuery( Expression expression, Type rowType )
    {
    return new QueryableImpl<T>( this, rowType, expression );
    }

  public <T> T execute( Expression expression, Class<T> type )
    {
    throw new UnsupportedOperationException();
    }

  public <T> T execute( Expression expression, Type type )
    {
    throw new UnsupportedOperationException();
    }

  @Override
  public <T> Enumerator<T> executeQuery( Queryable<T> queryable )
    {
    return null;
    }

  /** Binds an expression to this query provider. */
  public static class QueryableImpl<T> extends BaseQueryable<T>
    {
    public QueryableImpl( FlowQueryProvider provider, Type elementType, Expression expression )
      {
      super( provider, elementType, expression );
      }

    @Override
    public String toString()
      {
      return "Queryable(expr=" + expression + ")";
      }
    }
  }