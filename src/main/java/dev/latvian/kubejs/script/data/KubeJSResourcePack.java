package dev.latvian.kubejs.script.data;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import dev.latvian.kubejs.script.ScriptType;
import net.minecraft.resources.IResourcePack;
import net.minecraft.resources.ResourcePackFileNotFoundException;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.resources.data.IMetadataSectionSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author LatvianModder
 */
public class KubeJSResourcePack implements IResourcePack
{
	private final File folder;
	private final ResourcePackType packType;

	public KubeJSResourcePack(File f, ResourcePackType t)
	{
		folder = f;
		packType = t;
	}

	private static String getFullPath(ResourcePackType type, ResourceLocation location)
	{
		return String.format("%s/%s/%s", type.getDirectoryName(), location.getNamespace(), location.getPath());
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public InputStream getRootResourceStream(String fileName) throws IOException
	{
		if (fileName.equals("pack.png"))
		{
			return KubeJSResourcePack.class.getResourceAsStream("/assets/kubejs/textures/logo.png");
		}

		throw new ResourcePackFileNotFoundException(folder, fileName);
	}

	@Override
	public InputStream getResourceStream(ResourcePackType type, ResourceLocation location) throws IOException
	{
		String resourcePath = getFullPath(type, location);

		if (type != packType)
		{
			throw new IllegalStateException(packType.getDirectoryName() + " KubeJS pack can't load " + resourcePath + "!");
		}

		File file = new File(folder, resourcePath);

		if (file.exists())
		{
			return new BufferedInputStream(new FileInputStream(file));
		}

		throw new ResourcePackFileNotFoundException(folder, resourcePath);
	}

	@Override
	public boolean resourceExists(ResourcePackType type, ResourceLocation location)
	{
		return type == packType && new File(folder, getFullPath(type, location)).exists();
	}

	@Override
	public Collection<ResourceLocation> getAllResourceLocations(ResourcePackType type, String pathIn, int maxDepth, Predicate<String> filter)
	{
		if (type != packType)
		{
			return Collections.emptySet();
		}

		File file1 = new File(folder, type.getDirectoryName());
		List<ResourceLocation> list = Lists.newArrayList();

		for (String s : getResourceNamespaces(type))
		{
			getAllResourceLocations0(new File(new File(file1, s), pathIn), maxDepth, s, list, pathIn + "/", filter);
		}

		return list;
	}

	private void getAllResourceLocations0(File file, int maxDepth, String originPath, List<ResourceLocation> list, String path, Predicate<String> filter)
	{
		File[] files = file.listFiles();

		if (files == null || files.length == 0)
		{
			return;
		}

		for (File f : files)
		{
			if (f.isDirectory())
			{
				if (maxDepth > 0)
				{
					getAllResourceLocations0(f, maxDepth - 1, originPath, list, path + f.getName() + "/", filter);
				}
			}
			else if (!f.getName().endsWith(".mcmeta") && filter.test(f.getName()))
			{
				try
				{
					list.add(new ResourceLocation(originPath, path + f.getName()));
				}
				catch (ResourceLocationException ex)
				{
					(packType == ResourcePackType.CLIENT_RESOURCES ? ScriptType.CLIENT : ScriptType.SERVER).console.error(ex.getMessage());
				}
			}
		}
	}

	@Override
	public Set<String> getResourceNamespaces(ResourcePackType type)
	{
		if (type != packType)
		{
			return Collections.emptySet();
		}

		File file = new File(folder, type.getDirectoryName());

		if (file.exists() && file.isDirectory())
		{
			File[] list = file.listFiles();

			if (list != null && list.length > 0)
			{
				HashSet<String> namespaces = new HashSet<>();

				for (File f : list)
				{
					if (f.isDirectory())
					{
						namespaces.add(f.getName().toLowerCase());
					}
				}

				return namespaces;
			}
		}

		return Collections.emptySet();
	}

	@Nullable
	@Override
	public <T> T getMetadata(IMetadataSectionSerializer<T> serializer)
	{
		JsonObject json = new JsonObject();
		json.addProperty("description", "Magic");
		json.addProperty("pack_format", 4);
		json.addProperty("language", "en_us");
		return serializer.deserialize(json);
	}

	@Override
	public String getName()
	{
		return "KubeJS Resource Pack";
	}

	@Override
	public void close()
	{
	}
}
