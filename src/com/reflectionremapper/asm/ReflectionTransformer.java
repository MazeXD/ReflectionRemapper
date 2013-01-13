package com.reflectionremapper.asm;

import java.util.ArrayList;
import java.util.Collection;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.reflectionremapper.RemappedMethods;

import cpw.mods.fml.common.asm.ASMTransformer;
import cpw.mods.fml.common.registry.BlockProxy;

public class ReflectionTransformer extends ASMTransformer {
	private Collection<String> ignored = new ArrayList<String>();
	
	public ReflectionTransformer()
	{
		ignored.add("argo.");
		ignored.add("cpw.mods.fml.");
		ignored.add("net.minecraftforge.");
		ignored.add("org.bouncycastle.");
		ignored.add("paulscode.");
		
		RemappedMethods.loadMappings();
	}

	@Override
	public byte[] transform(String name, byte[] bytes) {
		for(String ignore : ignored)
		{
			if(name.startsWith(ignore))
			{
				return bytes;
			}
		}
		
		ClassReader reader = new ClassReader(bytes);
        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.EXPAND_FRAMES);
        
        transformNode(node);
        
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
		
		return writer.toByteArray();
	}
	
	public void transformNode(ClassNode node)
	{
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
						methodInstruction.owner = "com/reflectionremapper/RemappedMethods";
					}
				}
			}
		}
	}
}
