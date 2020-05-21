package org.dhp.common.utils;

import org.apache.commons.io.IOUtils;
import org.dhp.common.encrypt.MD5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FileUtils {

	static Logger logger = LoggerFactory.getLogger(FileUtils.class);

	public static DataInputStream createFileDataInputStream(String filepath) {
		try {
			File file = new File(filepath);
			return new DataInputStream(new FileInputStream(file));
		} catch (Exception e) {
			logger.error("找不到文件：{}", filepath);
		}
		return null;
	}

	public static DataOutputStream createFileDataOutputStream(String filepath) {
		return createFileDataOutputStream(filepath, false);
	}

	public static DataOutputStream createFileDataOutputStream(String filepath, boolean isAppend) {
		File file = new File(filepath);
		if (filepath.contains("/")) {
			File dir = new File(filepath.substring(0, filepath.lastIndexOf('/')));
			if (!dir.exists()) {
				dir.mkdirs();
			}
		}
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (Exception e) {
				logger.error("", e);
				return null;
			}
		}
		return createFileDataOutputStream(file, isAppend);
	}

	public static DataOutputStream createFileDataOutputStream(File file) {
		return createFileDataOutputStream(file, false);
	}

	public static DataOutputStream createFileDataOutputStream(File file, boolean isAppend) {
		try {
			return new DataOutputStream(new FileOutputStream(file, isAppend));
		} catch (Exception e) {
			logger.error("", e);
		}
		return null;
	}

	public static String readFile(InputStream input) throws IOException {
		ByteArrayOutputStream is = new ByteArrayOutputStream();
		// 一次读入一行，直到读入null为文件结束
		byte[] cache = new byte[1024];
		int len = input.read(cache);
		while (len > 0) {
			is.write(cache, 0, len);
			len = input.read(cache);
		}
		is.close();
		return new String(is.toByteArray(), "UTF-8");
	}

	public static String readFile(String filepath) {
		File file = new File(filepath);
		return readFile(file);
	}
	public static String readFile(File file)  {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			StringBuffer content = new StringBuffer();
			// 一次读入一行，直到读入null为文件结束
			char[] cache = new char[1024];
			int len = reader.read(cache);
			while (len > 0) {
				content.append(cache, 0, len);
				len = reader.read(cache);
			}
			reader.close();
			return content.toString();
		} catch (IOException e) {
			logger.info("文件不存在：" + file.getAbsolutePath());
			return null;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
				}
			}
		}
	}

	// 复制文件
	public static void copyFile(File sourceFile, File targetFile) throws IOException {
		BufferedInputStream inBuff = null;
		BufferedOutputStream outBuff = null;
		try {
			// 新建文件输入流并对它进行缓冲
			inBuff = new BufferedInputStream(new FileInputStream(sourceFile));

			// 新建文件输出流并对它进行缓冲
			outBuff = new BufferedOutputStream(new FileOutputStream(targetFile));

			// 缓冲数组
			byte[] b = new byte[1024 * 5];
			int len;
			while ((len = inBuff.read(b)) != -1) {
				outBuff.write(b, 0, len);
			}
			// 刷新此缓冲的输出流
			outBuff.flush();
		} finally {
			// 关闭流
			if (inBuff != null)
				inBuff.close();
			if (outBuff != null)
				outBuff.close();
		}
	}

	public static void clearFile(File file) throws IOException {
		FileWriter w = null;
		try {
			w = new FileWriter(file);
			w.write("");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (w != null)
				w.close();
		}
	}

	public static ArrayList<File> getFiles(File file) {
		return getFiles(file, null);
	}

	public static ArrayList<File> getFiles(File file, FileFilter filter) {
		ArrayList<File> ret = new ArrayList<File>();
		File[] files;
		if (filter != null)
			files = file.listFiles(filter);
		else
			files = file.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {
					ret.addAll(getFiles(f, filter));
				} else
					ret.add(f);
			}
		}
		return ret;
	}

	public static List<FileInfo> getFileInfos(File file) {
		List<FileInfo> ret = new ArrayList<FileInfo>();
		File[] files = file.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {
					ret.addAll(getFileInfos(f));
				} else {
					FileInfo info = new FileInfo();
					info.setFile(f);
					info.setName(f.getName());
					info.setMd5value(MD5Util.MD5File(f));
					ret.add(info);
				}
			}
		}
		return ret;
	}

	public static File plusFilename(String filename) {
		int index = filename.lastIndexOf('.');
		int i = 1;
		File file = new File(filename.substring(0, index) + "(" + (i++) + ")" + filename.substring(index));
		while (file.exists()) {
			file = new File(filename.substring(0, index) + "(" + (i++) + ")" + filename.substring(index));
		}
		return file;

	}

	public static String readLastLine(File file, String charset) throws IOException {
		if (!file.exists() || file.isDirectory() || !file.canRead()) {
			return null;
		}
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "r");
			long len = raf.length();
			if (len == 0L) {
				return "";
			} else {
				long pos = len - 1;
				while (pos > 0) {
					pos--;
					raf.seek(pos);
					if (raf.readByte() == '\n') {
						break;
					}
				}
				if (pos == 0) {
					raf.seek(0);
				}
				byte[] bytes = new byte[(int) (len - pos)];
				raf.read(bytes);
				if (charset == null) {
					return new String(bytes);
				} else {
					return new String(bytes, charset);
				}
			}
		} catch (FileNotFoundException e) {
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (Exception e2) {
				}
			}
		}
		return null;
	}

	public static byte[] readFileBytes(File file) {
		DataInputStream input = null;
		try {
			input = new DataInputStream(new FileInputStream(file));
			ByteArrayOutputStream is = new ByteArrayOutputStream();
			// 一次读入一行，直到读入null为文件结束
			byte[] cache = new byte[1024];
			int len = input.read(cache);
			while (len > 0) {
				is.write(cache, 0, len);
				len = input.read(cache);
			}
			is.close();
			return is.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e1) {
				}
			}
		}
	}
	
	public static byte[] readBytes(InputStream inputStream) {
        try {
            ByteArrayOutputStream is = new ByteArrayOutputStream();
            // 一次读入一行，直到读入null为文件结束
            byte[] cache = new byte[1024];
            int len = inputStream.read(cache);
            while (len > 0) {
                is.write(cache, 0, len);
                len = inputStream.read(cache);
            }
            is.close();
            return is.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e1) {
                }
            }
        }
    }


	final static Logger LOG = LoggerFactory.getLogger(FileUtils.class);

	/**
	 * 从指定位置读取文件内容,并写到输出流
	 * 
	 * @param filePath
	 *            文件全路径
	 * @param output
	 *            由调用者负责关闭
	 */
	public static void writeToOutput(String filePath, OutputStream output) {
		InputStream input = null;
		try {
			input = new BufferedInputStream(new FileInputStream(new File(filePath)));
			IOUtils.copy(input, output);
			output.flush();
			// output.close();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException("文件流写入失败");
		} finally {
			try {
				if (input != null) {
					input.close();
				}
			} catch (Exception e) {
				LOG.error("文件流关闭异常", e);
			}
		}
	}

	/**
	 * 从输入流读取内容，并写入到指定文件
	 * 
	 * @param input
	 *            由调用者负责关闭
	 * @param dir
	 *            指定文件目录
	 * @param fileName
	 *            指定文件名(包括后缀名)
	 */
	public static File writeToFile(InputStream input, String dir, String fileName) {
		OutputStream output = null;
		try {
			File dest = getFile(dir, fileName);
			output = new BufferedOutputStream(new FileOutputStream(dest));
			IOUtils.copy(input, output);
			return dest;
		} catch (Exception e) {
			throw new RuntimeException("写入文件失败");
		} finally {
			try {
				if (output != null) {
					output.close();
				}
			} catch (Exception e) {
				LOG.error("文件流关闭异常", e);
			}
		}
	}

	/**
	 * 从输入流读取内容，并写入到指定文件
	 * 
	 * @param data
	 * 
	 * @param dir
	 *            指定文件目录
	 * @param fileName
	 *            指定文件名(包括后缀名)
	 */
	public static File writeToFile(byte[] data, String dir, String fileName) {
		OutputStream output = null;
		try {
			File dest = getFile(dir, fileName);
			output = new BufferedOutputStream(new FileOutputStream(dest));
			IOUtils.write(data, output);
			return dest;
		} catch (Exception e) {
			throw new RuntimeException("写入文件失败");
		} finally {
			try {
				if (output != null) {
					output.close();
				}
			} catch (Exception e) {
				LOG.error("文件流关闭异常", e);
			}
		}
	}

	/**
	 * 获取文件对象,并将不存在的一系列父目录创建
	 * 
	 * @param dir
	 * @param fileName
	 * @return
	 */
	public static File getFile(String dir, String fileName) {
		File dest = new File(dir + File.separator + fileName);
		if (!dest.getParentFile().exists()) {
			dest.getParentFile().mkdirs();
		}
		return dest;
	}

	public static File getFile(String fileName) {
		File dest = new File(fileName);
		if (!dest.getParentFile().exists()) {
			dest.getParentFile().mkdirs();
		}
		return dest;
	}

	/**
	 * 从文件全路径中截取文件名(带后缀名)
	 * 
	 * @param filePath
	 * @return
	 */
	public static String getFileName(String filePath) {
		int slashIndex = filePath.lastIndexOf("/");
		return slashIndex > -1 ? filePath.substring(slashIndex + 1) : filePath;
	}

	/**
	 * 获得文件类型后缀
	 * 
	 * @param fileName
	 * @return
	 */
	public static String getFileExt(String fileName) {
		int index = fileName.lastIndexOf('.');
		return index > -1 ? fileName.substring(index + 1) : "";
	}

	/**
	 * 读取文本文件行数
	 * 
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	public static int getTxtLine(BufferedReader reader, Long fileLength) throws IOException {
		if (fileLength == 0l) {
			return 0;
		}
		LineNumberReader rf = new LineNumberReader(reader);
		rf.skip(fileLength);
		int result = rf.getLineNumber();
		rf.close();
		return result + 1;
	}

	public static class FileInfo {
		String name;
		File file;
		String md5value;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public File getFile() {
			return file;
		}

		public void setFile(File file) {
			this.file = file;
		}

		public String getMd5value() {
			return md5value;
		}

		public void setMd5value(String md5value) {
			this.md5value = md5value;
		}

		@Override
		public String toString() {
			return String.format("%s,\t%s,\t%s", md5value, name, file.getAbsolutePath());
		}
	}

	public static void writeToFile(byte[] data, String file) {
		OutputStream output = null;
		try {
			File dest = getFile(file);
			output = new BufferedOutputStream(new FileOutputStream(dest));
			IOUtils.write(data, output);
		} catch (Exception e) {
			throw new RuntimeException("写入文件失败");
		} finally {
			try {
				if (output != null) {
					output.close();
				}
			} catch (Exception e) {
				LOG.error("文件流关闭异常", e);
			}
		}
	}

	public static void createNewFile(File file) throws IOException {
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		file.createNewFile();
	}

	/**
	 * 
	 * @param resource
	 * @return
	 */
	public static InputStream readJarFileInputStream(String jarPath, String jarEntrySource) {
	    JarFile jarFile;
        try {
            jarFile = new JarFile(jarPath);
        } catch (IOException e) {
            return null;
        }
		return readJarFile(jarFile, jarEntrySource);
	}
	
	protected static InputStream readJarFile(JarFile jarFile, String jarEntrySource) {
	    try {
	        JarEntry jarEntry = null;
	        int jarIndex = jarEntrySource.indexOf("!");
            if(jarIndex > -1) {
                String interJarPath = jarEntrySource.substring(0, jarIndex);
                jarEntry = jarFile.getJarEntry(interJarPath);
                jarEntrySource = jarEntrySource.substring(jarIndex+2);
                
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                File file = File.createTempFile("jar_zip", "jar");
                DataOutputStream out = createFileDataOutputStream(file);
                byte[] buffer = new byte[1024];
                int len = inputStream.read(buffer);
                while(len>0) {
                    out.write(buffer, 0, len);
                    len = inputStream.read(buffer);
                }
                out.flush();
                out.close();
                jarFile = new JarFile(file);
                return readJarFile(jarFile, jarEntrySource);
            } else {
                jarEntry = jarFile.getJarEntry(jarEntrySource);
            }
            
            if (jarEntry == null) {
                return null;
            }
            return jarFile.getInputStream(jarEntry);
        } catch (

        IOException e) {
            e.printStackTrace();
            return null;
        }
	}
	
	public static FileLock lockFile(File file) {
		try {
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rws");
			FileChannel channel = randomAccessFile.getChannel();
			return channel.tryLock();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
		
	}
}
