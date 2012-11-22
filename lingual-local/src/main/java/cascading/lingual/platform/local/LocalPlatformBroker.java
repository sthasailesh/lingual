/*
 * Copyright (c) 2007-2012 Concurrent, Inc. All Rights Reserved.
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

package cascading.lingual.platform.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import cascading.flow.FlowConnector;
import cascading.flow.FlowProcess;
import cascading.flow.local.LocalFlowConnector;
import cascading.flow.local.LocalFlowProcess;
import cascading.lingual.catalog.SchemaCatalog;
import cascading.lingual.platform.PlatformBroker;
import cascading.scheme.local.TextLine;
import cascading.tap.SinkMode;
import cascading.tap.local.FileTap;
import cascading.tap.type.FileType;

/**
 *
 */
public class LocalPlatformBroker extends PlatformBroker<Properties>
  {
  public LocalPlatformBroker()
    {
    }

  @Override
  public String getName()
    {
    return "local";
    }

  @Override
  public Class<? extends SchemaCatalog> getCatalogClass()
    {
    return LocalCatalog.class;
    }

  @Override
  public FlowConnector getFlowConnector()
    {
    return new LocalFlowConnector( getProperties() );
    }

  @Override
  public FlowProcess<Properties> getFlowProcess()
    {
    return new LocalFlowProcess( getConfig() );
    }

  @Override
  public FileType getFileTypeFor( String identifier )
    {
    return new FileTap( new TextLine(), identifier, SinkMode.KEEP );
    }

  @Override
  public String[] getChildIdentifiers( FileType<Properties> fileType ) throws IOException
    {
    return fileType.getChildIdentifiers( getConfig() );
    }

  @Override
  public Properties getConfig()
    {
    return getProperties();
    }

  @Override
  public boolean pathExists( String path )
    {
    File file = new File( path );

    return file.exists();
    }

  @Override
  public boolean deletePath( String path )
    {
    File file = new File( path );

    return file.delete();
    }

  @Override
  protected InputStream getInputStream( String path )
    {
    if( path == null || path.isEmpty() )
      return null;

    File file = new File( path );

    if( !file.exists() )
      return null;

    try
      {
      return new FileInputStream( file );
      }
    catch( FileNotFoundException exception )
      {
      throw new RuntimeException( "unable to find file: " + path, exception );
      }
    }

  @Override
  protected OutputStream getOutputStream( String path )
    {
    if( path == null || path.isEmpty() )
      return null;

    File file = new File( path );

    if( file.exists() )
      file.delete();
    else
      file.getParentFile().mkdirs();

    try
      {
      return new FileOutputStream( file, false );
      }
    catch( FileNotFoundException exception )
      {
      throw new RuntimeException( "unable to find file: " + path, exception );
      }
    }

  protected String getFileSeparator()
    {
    return File.separator;
    }
  }
