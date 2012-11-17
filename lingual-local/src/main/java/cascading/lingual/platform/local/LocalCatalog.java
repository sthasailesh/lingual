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

import cascading.bind.catalog.DynamicSchema;
import cascading.bind.tap.TapResource;
import cascading.lingual.catalog.Format;
import cascading.lingual.catalog.LingualCatalog;
import cascading.lingual.catalog.Protocol;
import cascading.lingual.platform.LingualSchemeFactory;
import cascading.lingual.platform.PlatformBroker;
import cascading.lingual.tap.local.TypedTextDelimited;
import cascading.scheme.Scheme;
import cascading.scheme.local.TextDelimited;
import cascading.tap.SinkMode;
import cascading.tuple.Fields;

/**
 *
 */
public class LocalCatalog extends LingualCatalog
  {
  public LocalCatalog( PlatformBroker platformBroker )
    {
    super( platformBroker );
    }

  @Override
  public TapResource getResourceFor( String identifier, Protocol protocol, Format format, SinkMode mode )
    {
    return new LocalResource( identifier, protocol, format, mode );
    }

  @Override
  protected DynamicSchema.SchemeFactory getSchemeFactory()
    {
    return new LocalSchemeFactory();
    }

  private static class LocalSchemeFactory extends LingualSchemeFactory
    {
    @Override
    protected Scheme makeFileScheme( Format format, Fields fields )
      {
      switch( format )
        {
        case CSV:
          return new TextDelimited( fields, true, ",", "\"" );

        case TSV:
          return new TextDelimited( fields, true, "\t", "\"" );

        case TCSV:
          return new TypedTextDelimited( fields, ",", "\"" );

        case TTSV:
          return new TypedTextDelimited( fields, "\t", "\"" );

        default:
          return null;
        }
      }
    }
  }