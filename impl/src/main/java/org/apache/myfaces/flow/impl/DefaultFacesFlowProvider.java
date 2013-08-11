/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.flow.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.flow.Flow;
import org.apache.myfaces.spi.FacesFlowProvider;

/**
 *
 * @author Leonardo Uribe
 */
public class DefaultFacesFlowProvider extends FacesFlowProvider
{
    private static final String FLOW_PREFIX = "oam.flow";
    
    static final String FLOW_SESSION_MAP_SUBKEY_PREFIX = FLOW_PREFIX + ".SCOPE";
    
    /**
     * Token separator.
     */
    static final char SEPARATOR_CHAR = '.';
    
    private final static String CURRENT_FLOW_SCOPE_MAP = "oam.flow.SCOPE_MAP";

    @Override
    public Iterator<Flow> getAnnotatedFlows(FacesContext facesContext)
    {
        //Without CDI there is no @FlowDefinition annotations to scan for
        return null;
    }

    @Override
    public void doAfterEnterFlow(FacesContext facesContext, Flow flow)
    {
        // Reset current flow scope map
        facesContext.getAttributes().remove(CURRENT_FLOW_SCOPE_MAP);
    }

    @Override
    public void doBeforeExitFlow(FacesContext facesContext, Flow flow)
    {
        String flowMapKey = flow.getClientWindowFlowId(
            facesContext.getExternalContext().getClientWindow());
        String fullToken = FLOW_SESSION_MAP_SUBKEY_PREFIX + SEPARATOR_CHAR + flowMapKey;

        Map<Object, Object> map = (Map<Object, Object>) facesContext.getAttributes().get(
            CURRENT_FLOW_SCOPE_MAP);
        if (map != null)
        {
            map.clear();
        }
        else
        {
            map = (Map<Object, Object>) facesContext.getExternalContext().
                getSessionMap().get(fullToken);
            if (map != null)
            {
                map.clear();
            }
        }
        // Remove the map from session
        facesContext.getExternalContext().getSessionMap().remove(fullToken);
        
        // Reset current flow scope map
        facesContext.getAttributes().remove(CURRENT_FLOW_SCOPE_MAP);
    }

    @Override
    public Map<Object, Object> getCurrentFlowScope(FacesContext facesContext)
    {
        Map<Object, Object> map = (Map<Object, Object>) facesContext.getAttributes().get(
            CURRENT_FLOW_SCOPE_MAP);
        if (map == null)
        {
            Flow flow = facesContext.getApplication().getFlowHandler().getCurrentFlow(facesContext);
            if (flow != null)
            {
                String flowMapKey = flow.getClientWindowFlowId(
                    facesContext.getExternalContext().getClientWindow());
                
                map = new FlowScopeMap(this, flowMapKey);
                //String fullToken = FLOW_SESSION_MAP_SUBKEY_PREFIX + SEPARATOR_CHAR + flowMapKey;
                //map = createOrRestoreMap(facesContext, fullToken);
                
                facesContext.getAttributes().put(CURRENT_FLOW_SCOPE_MAP, map);
            }
        }
        return map;
    }
    
    /**
     * Create a new subkey-wrapper of the session map with the given prefix.
     * This wrapper is used to implement the maps for the flash scope.
     * For more information see the SubKeyMap doc.
     */
    Map<Object, Object> createOrRestoreMap(FacesContext context, String prefix,
        boolean create)
    {
        ExternalContext external = context.getExternalContext();
        Map<String, Object> sessionMap = external.getSessionMap();

        Map<Object, Object> map = (Map<Object, Object>) sessionMap.get(prefix);
        if (map == null && create)
        {
            map = new ConcurrentHashMap<Object, Object>();
            sessionMap.put(prefix, map);
        }
        return map;
    }
}
