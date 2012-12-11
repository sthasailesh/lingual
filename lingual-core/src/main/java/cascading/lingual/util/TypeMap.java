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

package cascading.lingual.util;

import java.io.Serializable;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 *
 */
public class TypeMap implements Serializable
  {
  final BiMap<String, Class> nameToType = HashBiMap.<String, Class>create();
  final BiMap<Class, String> typeToName = nameToType.inverse();

  protected void put( String name, Class type )
    {
    nameToType.put( name, type );
    }

  public Class getTypeFor( String name )
    {
    return nameToType.get( name );
    }

  public Class[] getTypesFor( String... names )
    {
    Class[] types = new Class[ names.length ];

    for( int i = 0; i < names.length; i++ )
      {
      types[ i ] = nameToType.get( names[ i ] );

      if( types[ i ] == null )
        throw new IllegalArgumentException( "no type found for name: " + names[ i ] );
      }

    return types;
    }

  public String getNameFor( Class type )
    {
    return typeToName.get( type );
    }

  public Map<String, Class> getNameToTypeMap()
    {
    return nameToType;
    }

  public Map<Class, String> getTypeToNameMap()
    {
    return typeToName;
    }
  }