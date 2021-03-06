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

package cascading.lingual.optiq;

import java.lang.reflect.Constructor;
import java.util.List;

import cascading.lingual.optiq.enumerable.CascadingFlowRunnerEnumerable;
import cascading.lingual.optiq.enumerable.CascadingValueInsertEnumerable;
import cascading.lingual.optiq.meta.Branch;
import cascading.lingual.optiq.meta.FlowHolder;
import cascading.lingual.optiq.meta.ValuesHolder;
import net.hydromatic.linq4j.expressions.BlockBuilder;
import net.hydromatic.linq4j.expressions.BlockExpression;
import net.hydromatic.linq4j.expressions.Expressions;
import net.hydromatic.optiq.impl.java.JavaTypeFactory;
import net.hydromatic.optiq.rules.java.EnumerableConvention;
import net.hydromatic.optiq.rules.java.EnumerableRel;
import net.hydromatic.optiq.rules.java.EnumerableRelImplementor;
import net.hydromatic.optiq.rules.java.PhysType;
import net.hydromatic.optiq.rules.java.PhysTypeImpl;
import org.eigenbase.rel.RelNode;
import org.eigenbase.rel.SingleRel;
import org.eigenbase.relopt.RelOptCluster;
import org.eigenbase.relopt.RelOptCost;
import org.eigenbase.relopt.RelOptPlanner;
import org.eigenbase.relopt.RelTraitSet;
import org.eigenbase.relopt.volcano.VolcanoPlanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
class CascadingEnumerableRel extends SingleRel implements EnumerableRel
  {
  private static final Logger LOG = LoggerFactory.getLogger( CascadingEnumerableRel.class );

  private PhysType physType;

  public CascadingEnumerableRel( RelOptCluster cluster, RelTraitSet traitSet, RelNode input )
    {
    super( cluster, traitSet, input );

    if( getConvention() != EnumerableConvention.ARRAY )
      throw new IllegalStateException( "unsupported convention " + getConvention() );

    physType = PhysTypeImpl.of( (JavaTypeFactory) cluster.getTypeFactory(), input.getRowType(), (EnumerableConvention) getConvention() );
    }

  public PhysType getPhysType()
    {
    return physType;
    }

  @Override
  public RelOptCost computeSelfCost( RelOptPlanner planner )
    {
    return super.computeSelfCost( planner ).multiplyBy( .1 );
    }

  @Override
  public RelNode copy( RelTraitSet traitSet, List<RelNode> inputs )
    {
    return new CascadingEnumerableRel( getCluster(), traitSet, sole( inputs ) );
    }

  @Override
  public BlockExpression implement( EnumerableRelImplementor implementer )
    {
    LOG.debug( "implementing enumerable" );

    CascadingRelNode input = (CascadingRelNode) getChild();
    Branch branch = input.visitChild( new Stack() );

    VolcanoPlanner planner = (VolcanoPlanner) getCluster().getPlanner();

    if( branch.tuples != null )
      return handleInsert( branch, planner );
    else
      return handleFlow( branch, planner );
    }

  private BlockExpression handleInsert( Branch branch, VolcanoPlanner planner )
    {
    ValuesHolder holder = new ValuesHolder( branch, planner );

    long ordinal = CascadingValueInsertEnumerable.addHolder( holder );

    Constructor<CascadingValueInsertEnumerable> constructor = getConstructorFor( CascadingValueInsertEnumerable.class );

    return new BlockBuilder().append( Expressions.new_( constructor, Expressions.constant( ordinal ) ) ).toBlock();
    }

  private BlockExpression handleFlow( Branch branch, VolcanoPlanner planner )
    {
    FlowHolder flowHolder = new FlowHolder( getPhysType(), branch, planner );

    long ordinal = CascadingFlowRunnerEnumerable.addHolder( flowHolder );

    Constructor<CascadingFlowRunnerEnumerable> constructor = getConstructorFor( CascadingFlowRunnerEnumerable.class );

    return new BlockBuilder().append( Expressions.new_( constructor, Expressions.constant( ordinal ) ) ).toBlock();
    }

  static <T> Constructor<T> getConstructorFor( Class<T> type )
    {
    Constructor<T> constructor;

    try
      {
      constructor = type.getConstructor( long.class );
      }
    catch( NoSuchMethodException exception )
      {
      throw new RuntimeException( exception );
      }

    return constructor;
    }
  }
