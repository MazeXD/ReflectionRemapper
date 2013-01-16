package mcpc.plus.transformers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import mcpc.plus.PluginRemapper;

import org.bukkit.plugin.java.PluginClassLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import cpw.mods.fml.common.FMLLog;

public class PluginTransformer {	
	private static final String[] RESERVED = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"}; // From FML's RelaunchClassLoader

	public static byte[] transformClass(String name, URLClassLoader loader) throws ClassNotFoundException
	{		
		if(!PluginRemapper.isInitialized()){
			PluginRemapper.initialize();
		}
		
		try
        {            
            byte[] bytes = getClassBytes(name, loader);
            
            bytes = transform(name, bytes);
            
            return bytes;
        }
        catch (Throwable e)
        {
            throw new ClassNotFoundException(name, e);
        }
	}
	
	private static byte[] getClassBytes(String name, URLClassLoader loader)
	{
        if (name.indexOf('.') == -1)
        {
            for (String res : RESERVED)
            {
                if (name.toUpperCase(Locale.ENGLISH).startsWith(res))
                {
                    byte[] data = getClassBytes("_" + name, loader);
                    if (data != null)
                    {
                        return data;
                    }
                }
            }
        }

        URL classResource = loader.findResource(name.replace('.', '/').concat(".class"));
        if (classResource == null)
        {
            return null;
        }
        
        try {
			return Resources.toByteArray(classResource);
		} catch (IOException e) {
		}
        
        return null;
	}
	
	private static byte[] transform(String name, byte[] bytes)
	{
		ClassNode node = new ClassNode();
		ClassReader reader = new ClassReader(bytes);
		
        reader.accept(node, ClassReader.EXPAND_FRAMES);
		
		if(transformNode(node))
		{
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
	        node.accept(writer);
	        
			bytes = writer.toByteArray();
		}
		
		return bytes;
	}
	
	private static boolean transformNode(ClassNode node)
	{
		boolean hasTransformed = false;
		
		for(Object temp : node.methods) {
			MethodNode method = (MethodNode) temp;
			
			for(int i = 0; i < method.instructions.size(); i++)
			{
				AbstractInsnNode instruction = method.instructions.get(i);
				
				if(instruction instanceof MethodInsnNode)
				{
					MethodInsnNode methodInstruction = (MethodInsnNode) instruction;
					
					String lookup = methodInstruction.owner + "." + methodInstruction.name;
					
					if(lookup.startsWith("java/lang/Class.forName"))
					{
						if(PluginRemapper.DEBUG)
						{ 
							System.out.println("Remapped in :" + node.name);
						}
						
						hasTransformed = true;
						methodInstruction.owner = "mcpc/plus/PluginRemapper";
					}
				}
			}
		}
		
		return hasTransformed;
	}
}
