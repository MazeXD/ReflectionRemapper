package com.reflectionremapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.Side;

public class RemappedMethods {
	private static Map<String, String> remapTable = new Hashtable<String, String>();
	private static Map<String, String> cache = new Hashtable<String, String>();
	
	public static Class forName(String className) throws ClassNotFoundException {
		if(cache.containsKey(className))
		{
			return Class.forName(cache.get(className));
		}
		
		String temp = className;
		
		if(remapTable.containsKey(className))
		{
			temp = remapTable.get(className);
		}
		else
		{
			for(Entry<String, String> entry : remapTable.entrySet())
			{
				if(className.startsWith(entry.getKey()))
				{
					temp = className.replace(entry.getKey(), entry.getValue());
				}
			}
		}
		
		try{
			Class result = Class.forName(temp);
			
			cache.put(className, temp);
			
			return result;
		}
		catch(ClassNotFoundException e)
		{
			FMLLog.severe("[ReflectionRemapper] Can't remap " + className + " to " + temp + " as " + temp + " doesn't exist");
		}
		
		return Class.forName(className);
	}

	public static Class forName(String name, boolean initialize, ClassLoader loader) throws ClassNotFoundException {
		if(cache.containsKey(name))
		{
			return Class.forName(cache.get(name), initialize, loader);
		}
		
		String temp = name;
		
		if(remapTable.containsKey(name))
		{
			temp = remapTable.get(name);
		}
		else
		{
			for(Entry<String, String> entry : remapTable.entrySet())
			{
				if(name.startsWith(entry.getKey()))
				{
					temp = name.replace(entry.getKey(), entry.getValue());
				}
			}
		}
		
		try{
			Class result = Class.forName(temp, initialize, loader);
			
			cache.put(name, temp);
			
			return result;
		}
		catch(ClassNotFoundException e)
		{
			FMLLog.severe("[ReflectionRemapper] Can't remap " + name + " to " + temp + " as " + temp + " doesn't exist");
		}
		
		return Class.forName(name, initialize, loader);
	}

	private static void readMapping(File file) throws IOException
	{
		// Reading based on FML's reading of access transformer files
		Resources.readLines(file.toURI().toURL(), Charsets.UTF_8, new LineProcessor<Void>()
        {
            @Override
            public Void getResult()
            {
                return null;
            }

            @Override
            public boolean processLine(String input) throws IOException
            {
                String line = Iterables.getFirst(Splitter.on('#').limit(2).split(input), "").trim();
                
                if (line.length() == 0)
                {
                    return true;
                }
                
                List<String> parts = Lists.newArrayList(Splitter.on(" ").trimResults().split(line));
                
                if (parts.size() != 2)
                {
                	FMLLog.severe("Invalid mapping line " + input);
                	return true;
                }

                remapTable.put(parts.get(0).replace("/", ".").replace("\\", "."), parts.get(1).replace("/", ".").replace("\\", "."));

                return true;
            }
        });
	}
	
	public static void loadMappings()
	{
		try {
			File configDir = new File((File) FMLInjectionData.data()[6], "config");
			configDir = configDir.getCanonicalFile();

			if (!configDir.exists()) {
				configDir.mkdirs();
			}

			File configFile = new File(configDir, "remapping.cfg");

			if (!configFile.exists()) {
				FileOutputStream output = null;
				InputStream input = null;

				try {
					output = new FileOutputStream(configFile);
					input = RemappedMethods.class.getResourceAsStream("remapping.cfg");

					if(input != null) {
						byte[] buffer = new byte[1024];
						int len = input.read(buffer);
	
						while (len != -1) {
							output.write(buffer, 0, len);
							len = input.read(buffer);
						}
						
						output.flush();
					}
				} finally {
					if (output != null) {
						output.close();
					}
					if (input != null) {
						input.close();
					}
				}
			}

			readMapping(configFile);
		} catch (IOException e) {
			FMLLog.severe("Failed to load remapping.cfg");
		}
	}
}
