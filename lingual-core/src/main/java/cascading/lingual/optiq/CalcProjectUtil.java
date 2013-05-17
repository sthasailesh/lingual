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
import java.util.HashSet;
import java.util.List;

import cascading.lingual.optiq.meta.Branch;
import cascading.operation.Filter;
import cascading.operation.Function;
import cascading.operation.Insert;
import cascading.operation.expression.ScriptFilter;
import cascading.operation.expression.ScriptTupleFunction;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.pipe.assembly.Rename;
import cascading.pipe.assembly.Retain;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import net.hydromatic.linq4j.expressions.BlockBuilder;
import net.hydromatic.linq4j.expressions.BlockExpression;
import net.hydromatic.linq4j.expressions.ConstantExpression;
import net.hydromatic.linq4j.expressions.Expression;
import net.hydromatic.linq4j.expressions.Expressions;
import net.hydromatic.optiq.impl.java.JavaTypeFactory;
import net.hydromatic.optiq.rules.java.RexToLixTranslator;
import org.eigenbase.rel.RelNode;
import org.eigenbase.rel.SingleRel;
import org.eigenbase.relopt.RelOptCluster;
import org.eigenbase.reltype.RelDataType;
import org.eigenbase.rex.RexInputRef;
import org.eigenbase.rex.RexLiteral;
import org.eigenbase.rex.RexNode;
import org.eigenbase.rex.RexProgram;
import org.eigenbase.rex.RexProgramBuilder;
import org.eigenbase.util.Pair;
import org.eigenbase.util.Permutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
class CalcProjectUtil
  {
  private static final Logger LOG = LoggerFactory.getLogger( CalcProjectUtil.class );

  static Branch resolveBranch( Stack stack, CascadingRelNode node, RexProgram program )
    {
    CascadingRelNode child = (CascadingRelNode) ( (SingleRel) node ).getChild();
    CalcProjectUtil.checkRowType( node );
    CalcProjectUtil.checkRowType( child );

    Branch branch = child.visitChild( stack );
    Pipe pipe = branch.current;
    RelOptCluster cluster = node.getCluster();

    boolean isPermutation = program.isPermutation();
    Permutation permutation = program.getPermutation();

    if( isPermutation && !permutation.isIdentity() )
      throw new UnsupportedOperationException( "reordering is not currently supported" );

    boolean isFilter = program.getCondition() != null;
    boolean isRename = ProgramUtil.isOnlyRename( program );
    boolean isComplex = ProgramUtil.isComplex( program );
    boolean onlyProjectsNarrow = ProgramUtil.isOnlyProjectsNarrow( program );
    boolean hasConstants = ProgramUtil.hasConstants( program );
    boolean hasFunctions = ProgramUtil.hasFunctions( program );

    Fields outgoingFields = RelUtil.createTypedFields( cluster, program.getOutputRowType() );

    if( isFilter )
      pipe = addFilter( cluster, program, pipe );

    if( isComplex || hasFunctions )
      pipe = addFunction( cluster, program, pipe, !isComplex );

    if( hasConstants && !isComplex )
      pipe = addConstants( node, program, pipe );

    if( isRename && !isComplex )
      pipe = addRename( cluster, program, pipe );

    if( onlyProjectsNarrow && !isComplex )
      outgoingFields = getNarrowFields( cluster, program );

    pipe = new Retain( pipe, outgoingFields );

    pipe = stack.addDebug( node, pipe );

    return new Branch( pipe, branch );
    }

  private static Pipe addRename( RelOptCluster cluster, RexProgram program, Pipe pipe )
    {
    RelDataType inputProjects = ProgramUtil.getInputProjectsRowType( program );
    Fields incomingFields = RelUtil.createTypedFields( cluster, inputProjects );

    Fields renameFields = getNarrowFields( cluster, program );

    return new Rename( pipe, incomingFields, renameFields );
    }

  private static Fields getNarrowFields( RelOptCluster cluster, RexProgram program )
    {
    RelDataType outputProjectsRowType = ProgramUtil.getOutputProjectsRowType( program );
    return RelUtil.createTypedFields( cluster, outputProjectsRowType );
    }

  private static Pipe addConstants( CascadingRelNode node, RexProgram program, Pipe pipe )
    {
    RelDataType constantsRowType = ProgramUtil.getOutputConstantsRowType( program );
    Fields constantFields = RelUtil.createTypedFields( node.getCluster(), constantsRowType );
    List<RexLiteral> constantsLiterals = ProgramUtil.getOutputConstantsLiterals( program );
    List<Object> values = ProgramUtil.asValues2( constantsLiterals );

    return new Each( pipe, Fields.NONE, new Insert( constantFields, values.toArray() ), Fields.ALL );
    }

  private static Pipe addFilter( RelOptCluster cluster, RexProgram program, Pipe pipe )
    {
    final Fields incomingFields = RelUtil.createTypedFields( cluster, program.getInputRowType() );

    BlockBuilder statements = new BlockBuilder();

    Expression condition = RexToLixTranslator.translateCondition(
      program,
      (JavaTypeFactory) cluster.getTypeFactory(),
      statements,
      new RexToLixTranslator.InputGetter()
      {
      public Expression field( BlockBuilder list, int index )
        {
        return Expressions.parameter( incomingFields.getType( index ), incomingFields.get( index ).toString() );
        }
      } );

    // if condition is constant and true, we don't need an expression filter to keep it around
    boolean keepsAllRecords = condition instanceof ConstantExpression && Boolean.TRUE.equals( ( (ConstantExpression) condition ).value );

    if( keepsAllRecords )
      return pipe;

    // create a filter to remove records that don't meet the expression
    Expression nullToFalse = Expressions.call( Functions.class, "falseIfNull", condition );
    Expression not = Expressions.not( nullToFalse ); // matches #isRemove semantics in Filter

    statements.add( Expressions.return_( null, not ) );
    BlockExpression block = statements.toBlock();
    String expression = Expressions.toString( block );

    LOG.debug( "filter parameters: {}", incomingFields );
    LOG.debug( "filter expression: {}", expression );

    Filter expressionFilter = new ScriptFilter( expression, incomingFields.getTypesClasses() ); // handles coercions

    return new Each( pipe, expressionFilter );
    }

  private static Pipe addFunction( RelOptCluster cluster, RexProgram program, Pipe pipe, boolean narrow )
    {
    final Fields incomingFields = RelUtil.createTypedFields( cluster, program.getInputRowType() );

    // only project the result of any expressions
    if( narrow )
      program = ProgramUtil.createNarrowProgram( program, cluster.getRexBuilder() );

    BlockBuilder statements = new BlockBuilder();

    List<Expression> expressionList = RexToLixTranslator.translateProjects(
      program,
      (JavaTypeFactory) cluster.getTypeFactory(),
      statements,
      new RexToLixTranslator.InputGetter()
      {
      public Expression field( BlockBuilder list, int index )
        {
        return Expressions.parameter( incomingFields.getType( index ), incomingFields.get( index ).toString() );
        }
      } );

    Expression record = Expressions.newArrayInit( Object.class, expressionList );

    record = Expressions.new_( getConstructor(), record );

    statements.add( Expressions.return_( null, record ) );

    BlockExpression block = statements.toBlock();
    String expression = Expressions.toString( block );

    Fields outgoingFields = RelUtil.createTypedFields( cluster, program.getOutputRowType() );

    LOG.debug( "function parameters: {}", incomingFields );
    LOG.debug( "function results: {}", outgoingFields );
    LOG.debug( "function expression: {}", expression );

    Function scriptFunction = new ScriptTupleFunction( outgoingFields, expression, incomingFields.getTypesClasses() );
    Fields outputSelector = narrow ? Fields.ALL : Fields.SWAP;

    return new Each( pipe, scriptFunction, outputSelector );
    }

  private static Constructor<Tuple> getConstructor()
    {
    try
      {
      return Tuple.class.getConstructor( Object[].class );
      }
    catch( NoSuchMethodException exception )
      {
      LOG.error( "unable to get constructor for Tuple" );
      throw new RuntimeException( exception );
      }
    }

  public static RexProgram createRexProgram( CascadingProjectRel projectRel )
    {
    RelOptCluster cluster = projectRel.getCluster();
    RelDataType rowType = projectRel.getChild().getRowType();

    RexProgramBuilder builder = new RexProgramBuilder( rowType, cluster.getRexBuilder() );

    List<Pair<String, RexNode>> projects = projectRel.projects();

    for( int i = 0; i < projects.size(); i++ )
      {
      Pair<String, RexNode> exp = projects.get( i );

      if( !( exp.right instanceof RexInputRef ) ) // RexCall or RexLiteral
        builder.addExpr( exp.right );

      int index = i;

      if( exp.right instanceof RexInputRef )
        index = ( (RexInputRef) exp.right ).getIndex();

      builder.addProject( index, exp.left );
      }

    return builder.getProgram( false ); // todo: optimizer causes issues
    }

  public static void checkRowType( RelNode rel )
    {
    final List<String> fieldNames = ProgramUtil.leftSlice( rel.getRowType().getFieldList() );
    if( new HashSet<String>( fieldNames ).size() < fieldNames.size() )
      throw new IllegalArgumentException( "field names are not unique in row type: " + rel.getRowType() );
    }
  }