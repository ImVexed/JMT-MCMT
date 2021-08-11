package org.jmt.mcmt.config;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.conversion.Path;
import com.electronwill.nightconfig.core.conversion.SpecIntInRange;
import com.electronwill.nightconfig.core.conversion.SpecNotNull;
import com.electronwill.nightconfig.core.conversion.SpecValidator;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.file.NoFormatFoundException;
import com.google.common.base.Predicates;

import net.minecraftforge.fml.loading.FMLPaths;

/**
 * 
 * So forge is being unhelpful, so I need to write my own damn config system
 * 
 * I'll try to use nightcoreconfig or whateverit'scalled as it's bundled
 * 
 * But may end up being stuck with JSON
 * 
 * 
 * 
 * @author jediminer543
 *
 */
public class SerDesConfig {

	/**
	 * Standard, run of the mill filter config
	 * 
	 * TODO: add more filter config parameters; like backup pools etc.
	 * 
	 * @author jediminer543
	 *
	 */
	public static class FilterConfig {
		@Path("priority")
		@SpecIntInRange(min = 0, max = Integer.MAX_VALUE)
		int priority;
		
		@Path("name")
		String name;
		
		@Path("targets.whitelist")
		@SpecValidator(ClassListValidator.class)
		List<String> whitelist;
		
		@Path("targets.blacklist")
		@SpecValidator(ClassListValidator.class)
		List<String> blacklist;
		
		@Path("pools.primary.name")
		@SpecNotNull
		String pool;
		
		@Path("pools.primary.params")
		//@SpecNotNull
		Map<String, String> poolParams;

		public int getPriority() {
			return priority;
		}

		public List<String> getWhitelist() {
			return whitelist;
		}

		public List<String> getBlacklist() {
			return blacklist;
		}

		public String getPool() {
			return pool;
		}

		public Map<String, String> getPoolParams() {
			return poolParams;
		}
	}
	
	public static class PoolConfig {
		@Path("class")
		@SpecValidator(ClassValidator.class)
		String clazz;
		
		@Path("name")
		@SpecNotNull
		String name;
		
		@Path("params")
		@SpecNotNull
		Map<String, String> initParams;
		
		@Path("priority")
		@SpecIntInRange(min = Integer.MIN_VALUE, max = Integer.MAX_VALUE)
		int priority;

		public String getClazz() {
			return clazz;
		}

		public String getName() {
			return name;
		}

		public Map<String, String> getInitParams() {
			return initParams;
		}

		public int getPriority() {
			return priority;
		}
		
	}
	
	private static final class ClassListValidator implements Predicate<Object> {
		String validatorRegex = "^[a-z]+(\\.[a-z0-9]+)*((\\.[A-Z][a-z0-9]+($[A-Za-z0-9]+)*)|\\.\\*|\\.\\*\\*)$";
		@Override
		public boolean test(Object t) {
			if (t == null) {
				return true;
			}
			if (t instanceof List<?>) {
				List<?> list = (List<?>) t;
				for (Object s : list) {
					if (!(s instanceof String && ((String)s).matches(validatorRegex))) {
						return false;
					}
				}
				return true;
			}
			return false;
		}
	}
	
	private static final class ClassValidator implements Predicate<Object> {
		String validatorRegex = "^[a-z]+(\\.[a-z0-9]+)*((\\.[A-Za-z0-9]+(\\$[A-Za-z0-9]+)*))?$";
		@Override
		public boolean test(Object s) {
			if ((s instanceof String && ((String)s).matches(validatorRegex))) {
				return true;
			}
			return false;
		}
	}
	
	static Map<String, FileConfig> configs = new HashMap<>();
	static Map<String, List<FilterConfig>> filters = new HashMap<>();
	static Map<String, List<PoolConfig>> pools = new HashMap<>();
	
	public static  void loadConfigs() {
		filters = new HashMap<>();
		pools = new HashMap<>();
		java.nio.file.Path cfgDir = FMLPaths.CONFIGDIR.get();
		java.nio.file.Path serdesDir = cfgDir.resolve("mcmt-serdes");
		if (Files.isDirectory(serdesDir)) {
			
		} else {
			try {
				Files.createDirectory(serdesDir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			Files.walk(serdesDir).map(p -> {
				try {
					return FileConfig.of(p);
				} catch (NoFormatFoundException nffe) {
					return null;
				}
			}).filter(Predicates.notNull()).forEach(SerDesConfig::loadConfig);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void loadConfig(FileConfig fc) {
		fc.load();
		String file = fc.getNioPath().getFileName().toString();
		List<Config> pool = fc.get("pools");
		List<Config> filter = fc.get("filters");
		if (pool == null) pool = new ArrayList<Config>();
		if (filter == null) filter = new ArrayList<Config>();
		ObjectConverter oc = new ObjectConverter();
		filters.put(file, filter.stream()
				.map(c->oc.toObject(c, FilterConfig::new))
				.collect(Collectors.toList()));
		pools.put(file, pool.stream()
				.map(c->oc.toObject(c, PoolConfig::new))
				.collect(Collectors.toList()));
		configs.put(file, fc);
	}
	
	public static List<PoolConfig> getPools() {
		return pools.values().stream()
		.flatMap(List::stream)
		.sorted(Comparator.comparingInt(PoolConfig::getPriority))
		.collect(Collectors.toList());
	}
	
	public static List<FilterConfig> getFilters() {
		return filters.values().stream()
		.flatMap(List::stream)
		.sorted(Comparator.comparingInt(FilterConfig::getPriority))
		.collect(Collectors.toList());
	}
	
	public static void saveConfigs() {
		//TODO
	}
}
