/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.sec.webctl;

import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;

import org.commonjava.badgr.shiro.web.BadgrShiroSetupListener;
import org.commonjava.util.logging.Logger;

@WebListener
public class ShiroSetupListener
    extends BadgrShiroSetupListener
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void contextInitialized( final ServletContextEvent sce )
    {
        logger.info( "Initializing BADGR Shiro authentication/authorization realm..." );
        super.contextInitialized( sce );
        logger.info( "...done." );
    }

}
