package mcpc.plus;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import mcpc.plus.transformers.PluginTransformer;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;

import cpw.mods.fml.common.FMLLog;

public class PluginRemapper {
	public static final boolean DEBUG = true;
	
	private static boolean initialized = false;
	private static HashMap<String, String> classes = Maps.newHashMap();
	private static HashMap<String, String> packages = Maps.newHashMap();
	private static HashMap<String, String> cache = Maps.newHashMap();
	private static PluginTransformer transformer;
	
	public static void initialize() {
		if(!initialized) 
		{
			readMapFile("mcpc/plus/resources/mappings.srg");
			
			transformer = new PluginTransformer();
		
			if(DEBUG)
			{
				FMLLog.fine("[PluginRemapper] Remapper has been initialized");
			}
			
			initialized = true;
		}
	}
	
	public static boolean isInitialized()
	{
		return initialized;
	}
	
	public static Class forName(String className) throws ClassNotFoundException {
		if(cache.containsKey(className))
		{
			return Class.forName(cache.get(className));
		}
		
		String newName = remap(className);
		
		if(newName != className) {
			try{
				Class result = Class.forName(newName);
				
				cache.put(className, newName);
				
				if(DEBUG)
				{
					FMLLog.fine("Remapped %s to %s", className, newName);
				}
				
				return result;
			}
			catch(ClassNotFoundException e)
			{
				FMLLog.severe("[PluginRemapper] Can't remap %s to %s as it doesn't exist", className, newName);
			}
		}
		
		cache.put(className, className);
		
		return Class.forName(className);
	}

	public static Class forName(String name, boolean initialize, ClassLoader loader) throws ClassNotFoundException {
		if(cache.containsKey(name))
		{
			return Class.forName(cache.get(name), initialize, loader);
		}
		
		String newName = remap(name);
		
		if(newName != name) {
			try{
				Class result = Class.forName(newName, initialize, loader);
				
				cache.put(name, newName);
				
				if(DEBUG)
				{
					FMLLog.fine("Remapped %s to %s", name, newName);
				}
				
				return result;
			}
			catch(ClassNotFoundException e)
			{
				FMLLog.severe("[PluginRemapper] Can't remap %s to %s as it doesn't exist", name, newName);
			}
		}
		
		cache.put(name, name);
		
		return Class.forName(name, initialize, loader);
	}
	
	private static String remap(String name)
	{
		String temp = name.replace('.', '/');
		
		if(classes.containsKey(temp))
		{
			return classes.get(temp).replace('/', '.');
		}
		else
		{
			for(Entry<String, String> entry : packages.entrySet())
			{
				String key = entry.getKey();
				
				if(temp.startsWith(key))
				{
					return temp.replace(key, entry.getValue()).replace('/', '.');
				}
			}
		}
		
		return name;
	}
	
	private static void readMapFile(String fileName) {
		try{
			URL resource = Resources.getResource(fileName);
	
			Resources.readLines(resource, Charsets.UTF_8, new LineProcessor<Void>() {
				@Override
				public Void getResult() {
					return null;
				}
	
				@Override
				public boolean processLine(String input) throws IOException {
					String line = input.trim();
					
					if (line.length() == 0) {
						return true;
					}
					
					List<String> parts = Lists.newArrayList(Splitter.on(" ").trimResults().split(line));
					
					String type = parts.get(0);
					
					if(type == "CL:")
					{
						if (parts.size() != 3) {
							throw new RuntimeException("Invalid config file line " + input);
						}
						
						classes.put(parts.get(1), parts.get(2));
					}
					else if(type == "PK:")
					{
						if (parts.size() != 3) {
							throw new RuntimeException("Invalid config file line " + input);
						}

						classes.put(parts.get(1), parts.get(2));
					}
					
					return true;
				}
			});
		}
		catch(IOException e)
		{
			FMLLog.severe("[PluginRemapper] Failed to read mappings.srg from resource");
		}
	}
}
