/**
 *  Copyright (C) 2007 - 2012 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.migrator;

import it.geosolutions.geostore.services.dto.ShortResource;
import it.geosolutions.geostore.services.dto.search.BaseField;
import it.geosolutions.geostore.services.dto.search.FieldFilter;
import it.geosolutions.geostore.services.dto.search.SearchFilter;
import it.geosolutions.geostore.services.dto.search.SearchOperator;
import it.geosolutions.geostore.services.rest.GeoStoreClient;
import it.geosolutions.geostore.services.rest.model.RESTResource;
import it.geosolutions.geostore.services.rest.model.ShortResourceList;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

/**
 * Simple application to manage MapStore's configuration migration between different version.
 *  
 * @author Tobia Di Pisa at tobia.dipisa@geo-solutions.it
 *
 */
public class MapStoreConfigurationMigrator {
	
	private final static Logger LOGGER = Logger.getLogger(MapStoreConfigurationMigrator.class
			.getSimpleName());
	
	private final static Properties properties = new Properties();
	
	private static String geostoreURL = null;
	
	private static String user = null;
	
	private static String password = null;
	
	private final static String mapStoreVersion = "1.3";

	/**
	 * Main procedure that uses the GeoSolutions GeoStoreClient in order to perform REST 
	 * request and update MapStore's JSON configurations.
	 * This main method read from remote GeoStore all resources and one by one update 
	 * the stored data accordingly the MapStore current behavior.
	 * 
	 * @param args
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws IOException, ParseException {
        
		loadConfiguration();
		
		if(geostoreURL == null || user == null || password == null){
			if (LOGGER.isLoggable(Level.SEVERE))
				LOGGER.log(Level.SEVERE, "Missing one or more mandatory properties !");
			return;
		}else{
			if (LOGGER.isLoggable(Level.INFO)) {
				LOGGER.log(Level.INFO, "Starting configuring the MapStore migration to " + mapStoreVersion + " version");
			}

			// //////////////////////////////////////////////
			// Initialize the GeoStore Client with provided 
			// connection properties.
			// //////////////////////////////////////////////
			
	        GeoStoreClient client = new GeoStoreClient();
	        
	        client.setGeostoreRestUrl(geostoreURL);
	        client.setUsername(user);
	        client.setPassword(password);
	        
			if (LOGGER.isLoggable(Level.INFO)) {
				LOGGER.log(Level.INFO, "Getting configurations IDs");
			}
	        
			// ///////////////////////////////////////////
			// Filter that match all GeoStore Resources
			// ///////////////////////////////////////////
			
	        SearchFilter searchFilter = new FieldFilter(BaseField.NAME, "%", SearchOperator.LIKE);
	        ShortResourceList resources = client.searchResources(searchFilter);
	        
	        List<ShortResource> list = resources.getList();
	        
	        int size = list.size();
	        for(int i=0; i<size; i++){
	        	ShortResource resource = list.get(i);
	        
	        	Long id = resource.getId();
	        	
	            String jsonData = client.getData(id);
	            
	    		if (LOGGER.isLoggable(Level.CONFIG)) {
	    			LOGGER.log(Level.CONFIG, "MapStore configuration loaded: " + jsonData + "\n");
	    		}
	            
	    		JSONObject json = migrate(jsonData, id);
	    		
	    		if(json == null){
	    			return;
	    		}
	        	
	    		if (LOGGER.isLoggable(Level.INFO)) {
	    			LOGGER.log(Level.INFO, "Saving new configuration with id: " + id);
	    		}
	        	
	    		// ////////////////////////////////////////////////////////
	    		// At the end save stored data and related resource only 
	    		// to update the 'lastupdate' resource attribute.
	    		// ////////////////////////////////////////////////////////
	    		
	    		RESTResource r = new RESTResource();
	        	client.updateResource(id, r);
	    		
	    		client.updateData(id, json.toString());
	        }
		}
	}

	/**
	 * @param jsonData
	 * @return JSONObject
	 * @throws ParseException 
	 */
	private static JSONObject migrate(String jsonData, Long id) throws ParseException {
		JSONParser parser = new JSONParser(JSONParser.MODE_RFC4627);
		JSONObject json = (JSONObject)parser.parse(jsonData);
		
        // //////////////////////////////////
        // Starting with migration
        // //////////////////////////////////
        
		if (LOGGER.isLoggable(Level.INFO)) {
			LOGGER.log(Level.INFO, "Starting configurations migration with id: " + id + "\n");
		}
        
        if(json.containsKey("scaleOverlayUnits")){
        	json.remove("scaleOverlayUnits");
        }
        
        if(json.containsKey("cswconfig")){
        	json.remove("cswconfig");
        }
        
        if(json.containsKey("xmlJsontranslate")){
        	json.remove("xmlJsontranslate");
        }
        
        if(json.containsKey("tools")){
        	json.remove("tools");
        }
        
        if(json.containsKey("customTools")){
        	json.remove("customTools");
        }
        
        if(json.containsKey("viewerTools")){
        	json.remove("viewerTools");
        }
        
        if(json.containsKey("georeferences")){
        	json.remove("georeferences");
        }
        
        if(json.containsKey("customPanels")){
        	json.remove("customPanels");
        }
        
        if(json.containsKey("map")){
        	JSONObject jsonMap = (JSONObject)json.get("map");
        	
        	String projection = null;
        	if(jsonMap.containsKey("projection")){
        		projection = (String)jsonMap.get("projection");
        	}else{
        		if (LOGGER.isLoggable(Level.SEVERE)) {
        			LOGGER.log(Level.SEVERE, "Incorrect Configuration: 'projection' element must exists inside 'map'!" + "\n");
        		}
            	return null;       	
            }

        	if(projection != null && jsonMap.containsKey("maxExtent")){
        		if(projection.equals("ESPG:4326") || projection.equals("EPSG:900913")){
        			if(!jsonMap.containsKey("extent")){
        				jsonMap.put("extent", jsonMap.get("maxExtent"));
        			}
        			
        			jsonMap.remove("maxExtent");
        		}else{
        			// /////////////////////////////////////////////////////////////////////////////
        			// In this case the 'maxExtent' must be specified (OL manages by default only 
        			// EPSG:4326 and EPSG:900913). We assume that the 'maxExtent' is correct and 
        			// corresponding to the entire bounds of the used projection.
        			// /////////////////////////////////////////////////////////////////////////////
        		}
        	}
        }else{
    		if (LOGGER.isLoggable(Level.SEVERE)) {
    			LOGGER.log(Level.SEVERE, "Incorrect Configuration: 'map' element must exists!" + "\n");
    		}
        	return null;         	
        }
        
		if (LOGGER.isLoggable(Level.CONFIG)) {
			LOGGER.log(Level.CONFIG, "MapStore configuration modified: " + json.toString() + "\n");
		}		
		
		return json;
	}

	/**
	 * Load the properties configuration in order to set the GeoStore Client connection parameters.
	 * 
	 * @throws IOException
	 */
	private static void loadConfiguration() throws IOException {
        InputStream inputStream = MapStoreConfigurationMigrator.class.getClassLoader().getResourceAsStream("migration.properties");
        
        // /////////////////////////////
        // Load properties from file
        // /////////////////////////////
        
		try {
			properties.load(inputStream);
		} catch (IOException e) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.log(Level.SEVERE,"Error encountered while processing properties file", e);
			}
		} finally {
			try {
				if (inputStream != null)
					inputStream.close();
			} catch (IOException e) {
				if (LOGGER.isLoggable(Level.SEVERE))
					LOGGER.log(Level.SEVERE,"Error building the migration configuration ", e);
				throw new IOException(e.getMessage());
			}
		}
		
		geostoreURL = properties.getProperty("geostoreURL");
		user = properties.getProperty("user");
		password = properties.getProperty("password");		
	}
}