/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.camel.Component;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.api.management.mbean.ManagedResourceEndpointMBean;
import org.apache.camel.converter.IOConverter;
import org.apache.camel.impl.ProcessorEndpoint;
import org.apache.camel.util.IOHelper;
import org.apache.camel.util.ResourceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A useful base class for endpoints which depend on a resource
 * such as things like Velocity or XQuery based components.
 */
@ManagedResource(description = "Managed ResourceEndpoint")
public abstract class ResourceEndpoint extends ProcessorEndpoint implements ManagedResourceEndpointMBean {
    protected final transient Logger log = LoggerFactory.getLogger(getClass());
    private String resourceUri;
    private boolean contentCache;
    private volatile byte[] buffer;

    public ResourceEndpoint() {
    }

    public ResourceEndpoint(String endpointUri, Component component, String resourceUri) {
        super(endpointUri, component);
        this.resourceUri = resourceUri;
    }

    /**
     * Gets the resource as an input stream considering the cache flag as well.
     * <p/>
     * If cache is enabled then the resource content is cached in an internal buffer and this content is
     * returned to avoid loading the resource over and over again.
     *
     * @return the input stream
     * @throws IOException is thrown if error loading the content of the resource to the local cache buffer
     */
    public InputStream getResourceAsInputStream() throws IOException {
        // try to get the resource input stream
        InputStream is;
        if (isContentCache()) {
            synchronized (this) {
                if (buffer == null) {
                    log.debug("Reading resource: {} into the content cache", resourceUri);
                    is = getResourceAsInputStreamWithoutCache();
                    buffer = IOConverter.toBytes(is);
                    IOHelper.close(is, resourceUri, log);
                }
            }
            log.debug("Using resource: {} from the content cache", resourceUri);
            return new ByteArrayInputStream(buffer);
        }

        return getResourceAsInputStreamWithoutCache();
    }

    protected InputStream getResourceAsInputStreamWithoutCache() throws IOException {
        return loadResource(resourceUri);
    }

    /**
     * Loads the given resource.
     *
     * @param uri uri of the resource.
     * @return the loaded resource
     * @throws IOException is thrown if resource is not found or cannot be loaded
     */
    protected InputStream loadResource(String uri) throws IOException {
        return ResourceHelper.resolveMandatoryResourceAsInputStream(getCamelContext().getClassResolver(), uri);
    }

    public boolean isContentCache() {
        return contentCache;
    }
    
    public synchronized void clearContentCache() {
        log.debug("Clearing resource: {} from the content cache", resourceUri);
        buffer = null;
    }

    /**
     * Sets whether to use resource content cache or not - default is <tt>false</tt>.
     *
     * @see #getResourceAsInputStream()
     */
    public void setContentCache(boolean contentCache) {
        this.contentCache = contentCache;
    }

    public String getResourceUri() {
        return resourceUri;
    }

    public void setResourceUri(String resourceUri) {
        this.resourceUri = resourceUri;
    }
}