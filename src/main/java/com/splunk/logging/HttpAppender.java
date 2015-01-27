package com.splunk.logging;
/*
 * Copyright 2013-2014 Splunk, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"): you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Booleans;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.entity.StringEntity;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Splunk Http Appender.
 */
@Plugin(name = "Http", category = "Core", elementType = "appender", printObject = true)
public final class HttpAppender extends AbstractAppender
{
	private final String _url;
	private final String _token;
	private final String _source;
	private final String _sourcetype;
	private final String _index;
	
	private CloseableHttpClient _client = HttpClients.createDefault();
	
	private HttpAppender(final String name, final String url, final String token, 
			             final String source, final String sourcetype, final String index, final Filter filter, 
			             final Layout<? extends Serializable> layout, final boolean ignoreExceptions)
	{
		super(name, filter, layout, ignoreExceptions);
		_url = url;
		_token = token;
		_source = source;
		_sourcetype = sourcetype;
		_index = index;
	}
			
	/**
     * Create a Http Appender.
     * @param Url: The Url of the splunk instance to log events to.
     * @param Token: The token of the application 
     * @param ignore If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise
     *               they are propagated to the caller.
     * @param layout The layout to use to format the event. If no layout is provided the default PatternLayout
     * will be used.
     * @return The Splunk-Http Appender.
     */
    @PluginFactory
    public static HttpAppender createAppender(
            // @formatter:off
            @PluginAttribute("Url") final String url,
            @PluginAttribute("protocol") final String protocol,            
            @PluginAttribute("token") final String token,
            @PluginAttribute("name") final String name,
            @PluginAttribute("source") final String source,
            @PluginAttribute("sourcetype") final String sourcetype, 
            @PluginAttribute("index") final String index,             
            @PluginAttribute("ignoreExceptions") final String ignore,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter
    		)
    {
    	if (name == null)
    	{
            LOGGER.error("No name provided for HttpAppender");
            return null;
        }
    	
    	if (url == null)
    	{
            LOGGER.error("No Splunk URL provided for HttpAppender");
            return null;
        }
    	
    	if (token == null)
    	{
    	    LOGGER.error("No token provided for HttpAppender");
            return null;
        }
    	
    	if (protocol == null)
    	{
    	    LOGGER.error("No valid protocol provided for HttpAppender");
            return null;
        }
    	
    	if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
    	
    	final boolean ignoreExceptions = true;
    	
    	return new HttpAppender(name, protocol + "://" + url, token, source, sourcetype, index, filter, layout, ignoreExceptions);    	
    }
    
   
    /**
     * Perform Appender specific appending actions.
     * @param event The Log event.
     */
    @Override
    public void append(final LogEvent event)
    {   
    	try
    	{
    		JSONObject obj = new JSONObject();
    	
    		if (_source != null)
    			obj.put("source", _source);
    	
    		if (_sourcetype != null)
    			obj.put("sourcetype", _sourcetype);
    	
    		if (_index != null)
    			obj.put("index", _index);
    		
    		JSONObject obj2 = new JSONObject();
    		    	
        	String evt = new String(getLayout().toByteArray(event), "UTF-8");
    		obj2.put("message", evt);
    		
    		obj.put("event", obj2);
    		
    		String msg = obj.toJSONString();
    		
    		HttpPost post = new HttpPost(_url);
	    	post.setHeader("Authorization", "Splunk " + _token);
	    	StringEntity entity = new StringEntity(msg, "utf-8");
	    	entity.setContentType("text/plain; charset=utf-8"); 
	    	post.setEntity(entity);

	    	HttpResponse response = _client.execute(post);    	
	    	int responseCode = response.getStatusLine().getStatusCode();
    	}
    	catch(IOException e )
    	{
    		LOGGER.error("IO exception when sending requests");		
    	}
    }
    
    /**
     * Stop the appender. Close the connection.
     */
    @Override
    public void stop()
    {
		try
		{
			_client.close();
		}
		catch(IOException e)
		{
    		LOGGER.error("IO exception when closing");			
		}
		
		super.stop();
    }    
}



