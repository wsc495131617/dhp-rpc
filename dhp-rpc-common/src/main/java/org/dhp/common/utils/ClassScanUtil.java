package org.dhp.common.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassScanUtil {
	private static final String CLASS_FILE_TAILS = ".class";

	private static final Logger logger = LoggerFactory.getLogger(ClassScanUtil.class);

	private static Map<String, Set<Class<?>>> package2Classes = new ConcurrentHashMap<String, Set<Class<?>>>();

	public static Set<Class<?>> getClassesByPackageName(String packageName, ClassFilter filter) throws IOException {
		Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
		if (StringUtils.isEmpty(packageName)) {
			return classes;
		}
		if (package2Classes.containsKey(packageName)) {
			Set<Class<?>> result = (Set<Class<?>>) package2Classes.get(packageName);
			if (result != null) {
				return result;
			}
		}
		getClassesByPackageName(classes, packageName, filter);
		package2Classes.put(packageName, classes);
		return classes;
	}

	public static void clearCache() {
		package2Classes = new ConcurrentHashMap<String, Set<Class<?>>>();
	}

	private static void getClassesByPackageName(Set<Class<?>> classes, String packageName, ClassFilter filter)
			throws IOException {
		boolean recursive = true;
		String packageDirName = packageName.replace('.', File.separatorChar);

		Enumeration<?> dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);

		if (filter == null) {
			filter = new ClassFilter() {
				public boolean accept(Class<?> clz) {
					return true;
				}
			};
		}

		while (dirs.hasMoreElements()) {
			URL url = (URL) dirs.nextElement();
			String protocol = url.getProtocol();
			if ("file".equals(protocol)) {
				String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
				findAndAddClassesInPackageByFile(packageName, filePath, recursive, classes, filter);
			} else if ("jar".equals(protocol)) {
				JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
				Enumeration<?> entries = jar.entries();
				String pName = packageName;
				while (entries.hasMoreElements()) {
					JarEntry entry = (JarEntry) entries.nextElement();
					String name = entry.getName();
					if (name.charAt(0) == File.separatorChar) {
						name = name.substring(1);
					}
					if (name.startsWith(packageDirName)) {
						int idx = name.lastIndexOf(File.separatorChar);
						if (idx != -1) {
							pName = name.substring(0, idx).replace(File.separatorChar, '.');
						}

						if (((idx != -1) || (recursive)) && (name.endsWith(CLASS_FILE_TAILS))
								&& (!entry.isDirectory())) {
							String className = name.substring(pName.length() + 1, name.length() - 6);

							tryAddClass(classes, Thread.currentThread().getContextClassLoader(),
									pName + '.' + className, filter);
						}
					}
				}
			}
		}
	}

	private static void findAndAddClassesInPackageByFile(String packageName, String packagePath,
                                                         final boolean recursive, Set<Class<?>> classes, ClassFilter filter) {
		File dir = new File(packagePath);
		if ((!dir.exists()) || (!dir.isDirectory())) {
			return;
		}

		File[] dirfiles = dir.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return (recursive && file.isDirectory()) || (file.getName().endsWith(CLASS_FILE_TAILS));
			}
		});
		if (dirfiles != null) {
			for (File file : dirfiles) {
				if (file.isDirectory()) {
					findAndAddClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(),
							recursive, classes, filter);
				} else {
					String className = file.getName().substring(0, file.getName().length() - 6);

					tryAddClass(classes, Thread.currentThread().getContextClassLoader(), packageName + '.' + className,
							filter);
				}
			}
		}
	}

	private static void tryAddClass(Set<Class<?>> classes, ClassLoader loader, String className, ClassFilter filter) {
		try {
			Class<?> clz = loader.loadClass(className);
			if (filter.accept(clz)) {
				classes.add(clz);
			}
		} catch (Throwable e) {
			logger.warn("Failed to load class[" + className + "]", e);
		}
	}

	public static abstract interface ClassFilter {
		public abstract boolean accept(Class<?> paramClass);
	}

	private static void findAndAddFileInPackageByFile(String packageName, String packagePath, String fileMatch, Set<String> files)
	{
		File dir = new File(packagePath);
		if ((!dir.exists()) || (!dir.isDirectory())) {
			return;
		}

		File[] dirfiles = dir.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory() || file.getName().matches(fileMatch);
			}
		});
		if(dirfiles != null) {
			for (File file : dirfiles){
				if (file.isDirectory()) {
					findAndAddFileInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), fileMatch, files);
				}
				else
				{
					files.add(file.getAbsolutePath());
				}
			}	
		}
	}
	
	private static void findAndAddFileInPackageByFile2(String packageName, String packagePath, String fileMatch, Set<String> files)
    {
        File dir = new File(packagePath);
        if ((!dir.exists()) || (!dir.isDirectory())) {
            return;
        }

        File[] dirfiles = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory() || file.getName().matches(fileMatch);
            }
        });
        if(dirfiles != null) {
            for (File file : dirfiles){
                if (file.isDirectory()) {
                    findAndAddFileInPackageByFile2(packageName, file.getAbsolutePath(), fileMatch, files);
                }
                else
                {
                    if(org.dhp.common.utils.StringUtils.match("*"+packageName+"*", file.getAbsolutePath()))
                        files.add(file.getAbsolutePath());
                }
            }   
        }
    }
	
	public static Set<String> getFiles(String packageName, String fileMatch)  throws IOException {
		String packageDirName = packageName.replace('.', File.separatorChar);
		
		String resPath = packageDirName;
		//如果包有通配，那么getResources不支持通配
		if(packageDirName.contains("*")) {
		    resPath = resPath.substring(0, resPath.indexOf("*"));
		}
		
		Enumeration<?> dirs = Thread.currentThread().getContextClassLoader().getResources(resPath);

		Set<String> files = new HashSet<>();

		while (dirs.hasMoreElements()) {
			URL url = (URL) dirs.nextElement();
			String protocol = url.getProtocol();
			if ("file".equals(protocol)) {
				String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
				findAndAddFileInPackageByFile2(packageDirName, filePath, fileMatch, files);
			} else if ("jar".equals(protocol)) {
				String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
				JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
				Enumeration<?> entries = jar.entries();
				String pName = packageName;
				while (entries.hasMoreElements()) {
					JarEntry entry = (JarEntry) entries.nextElement();
					String name = entry.getName();
					if (name.charAt(0) == File.separatorChar) {
						name = name.substring(1);
					}
					if (name.startsWith(resPath)) {
						int idx = name.lastIndexOf(File.separatorChar);
						if (idx != -1) {
							pName = name.substring(0, idx).replace(File.separatorChar, '.');
						}
						if (((idx != -1)) && (name.matches(fileMatch)) && (!entry.isDirectory())) {
							files.add(filePath+name.replace(packageDirName, ""));
						}
					}
				}
			}
		}
		return files;
	}
}