package net.tomato.component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

/**
 * 文件名称处理工具类<BR>
 * 是对Jakarta Commons-IO的补充。<BR>
 */

public final class FilenameUtil {

	public static boolean onNetWare = OsUtil.isFamily("netware");
	public static boolean onDos = OsUtil.isFamily("dos");

	private FilenameUtil() {
		super();
	}

	/**
	 * 得到相对路径。<BR>
	 * @param r_FolderName
	 * @param r_ResourceName
	 * @return
	 */
	public static String getRelativePath(String r_FolderName, String r_ResourceName) {
		if (null == r_FolderName || null == r_ResourceName) {
			return null;
		}
		String t_FolderName = normalizeInUnixStyle(r_FolderName);
		String t_ResourceName = normalizeInUnixStyle(r_ResourceName);

		return StringUtil.remove(t_ResourceName, t_FolderName);
	}

	/**
	 * 得到相对路径。<BR>
	 * @param r_FolderName
	 * @param r_ResourceName
	 * @return
	 */
	public static String getRelativePath(File r_Folder, File r_Resource) {
		if (null == r_Folder || null == r_Resource) {
			return null;
		}
		return getRelativePath(r_Folder.getAbsolutePath(), r_Resource.getAbsolutePath());
	}

	/**
	 * 将文件分隔符转成Unix样式。<BR>
	 * 如com//sun\jnlp\<BR>
	 * 就会变成com/sun/jnlp。<BR>
	 * @param path
	 * @return
	 */
	public static String normalizeInUnixStyle(String path) {
		path = FilenameUtils.normalize(path);
		return FilenameUtils.separatorsToUnix(path);
	}

	/**
	 * 将一个包名改成文件路径名。<BR>
	 * 
	 * Convert a package name to a path.<BR>
	 * 
	 * Example: org.test.name -> org/test/name
	 * 
	 * @param r_PackageName
	 * @return
	 */
	public static String toPathInUnixStyle(String r_PackageName) {
		String t_Path = StringUtil.replace(r_PackageName, ".", "/");
		t_Path = normalizeInUnixStyle(t_Path);
		if (null != t_Path && t_Path.startsWith("/")) {
			t_Path = StringUtil.removeStart(t_Path, "/");
		}
		if (null != t_Path && t_Path.endsWith("/")) {
			t_Path = StringUtil.removeEnd(t_Path, "/");
		}
		return t_Path;
	}

	/**
	 * 将一个文件路径名改成包名。<BR>
	 * 
	 * Convert a path to a package name.<BR>
	 * 
	 * Example: org/test/name.txt -> org.test.name.txt org\\test/name ->
	 * org.test.name
	 * 
	 * @param r_Path
	 * @return
	 */
	public static String toPackage(String r_Path) {
		String t_Path = normalizeInUnixStyle(r_Path);
		t_Path = StringUtil.replace(t_Path, "/", ".");
		if (null != t_Path && t_Path.startsWith(".")) {
			t_Path = StringUtil.removeStart(t_Path, ".");
		}

		if (null != t_Path && t_Path.endsWith(".")) {
			t_Path = StringUtil.removeEnd(t_Path, ".");
		}

		return t_Path;
	}

	/**
	 * 将一个文件路径名改成包名。<BR>
	 * 而且不带扩展名。
	 * 
	 * Convert a path to a package name without file extension.<BR>
	 * 
	 * Example: org/test/name.txt -> org.test.name org\\test/name ->
	 * org.test.name
	 * @param r_Path
	 * @return
	 */
	public static String toPackageWithoutExtension(String r_Path) {
		String t_Path = FilenameUtils.removeExtension(r_Path);
		t_Path = normalizeInUnixStyle(t_Path);
		t_Path = StringUtil.replace(t_Path, "/", ".");
		if (null != t_Path && t_Path.startsWith(".")) {
			t_Path = StringUtil.removeStart(t_Path, ".");
		}
		return t_Path;
	}

	/**
	 * 把一个类所在的包名路径化
	 * 
	 * @param clazz
	 *            the class
	 * @return 包名路径化
	 */
	public static String getPackagePath(Class<?> clazz) {
		return (clazz == null) ? null : (clazz.getPackage().getName().replace(".", "/") + "/");
	}

	/**
	 * 得到一个类对应的文件名。<BR>
	 * 
	 * Return the name in file format for a class.<BR>
	 * 
	 * @param className
	 *            the class name for being converted.
	 * 
	 */
	public static String getFileName(String className) {
		return "/" + StringUtil.replace(className, ".", File.separator);
	}

	/**
	 * 得到一个类对应的文件名。<BR>
	 * 
	 * Return the name in file format for a class.<BR>
	 * 
	 * @param clazz
	 *            the class for being converted.
	 * 
	 */
	public static String getFileName(Class<?> clazz) {
		return (clazz == null) ? null : ("/" + StringUtil.replace(clazz.getName(), ".", File.separator));
	}

	/**
	 * 取得跟clazz同一个目录下的文件的绝对路径
	 * 
	 * @param clazz
	 *            the class
	 * @param fileName
	 *            文件名称，相对于此类的文件目录
	 * @return 包名路径化
	 */
	public static String getAbsoluteFilePath(Class<?> clazz, String fileName) {
		String path = getPackagePath(clazz);
		if (path == null) {
			return null;
		}
		return clazz.getClassLoader().getResource(path + fileName).getFile();
	}

	/**
	 * 取得clazz的ClassPath的绝对路径
	 * 
	 * @param clazz
	 *            the class
	 * @return ClassPath的绝对路径
	 */
	public static String getAbsoluteClassPath(Class<?> clazz) {
		if (clazz == null) {
			clazz = FilenameUtil.class;
		}
		String qnamePath = clazz.getName().replace('.', '/');
		String currentPath = UrlUtil.getURL(qnamePath.concat(".class")).getFile();
		int index = currentPath.lastIndexOf(qnamePath);
		return currentPath.substring(0, index);
	}

	/**
	 * 取得file下面所有的文件列表
	 * 
	 * @param file
	 *            文件或者目录对象
	 * @return file下面所有的文件列表
	 */
	public static String[] getAllFileNames(File file) {
		List<String> fileList = new ArrayList<String>();
		getAllFileNames(file, fileList);
		return fileList.toArray(new String[0]);
	}

	/**
	 * 取得file下面所有的文件列表
	 * 
	 * @param file
	 *            文件或者目录对象
	 * @param fileNameList
	 *            file下面所有的文件列表
	 */
	public static void getAllFileNames(File file, List<String> fileNameList) {
		if (file.isFile()) {
			fileNameList.add(file.getAbsolutePath());
			return;
		} else {
			for (File f : file.listFiles()) {
				getAllFileNames(f, fileNameList);
			}
		}
	}

	/**
	 * 判断一个目录是否是另外一个资源的前缀。<BR>
	 * @param r_FolderName
	 * @param r_ResourceName
	 * @return
	 */
	public static boolean isPrefixOf(File folder, File file) {
		if (null == folder || null == file) {
			return false;
		}
		return isPrefixOf(folder.getAbsolutePath(), file.getAbsolutePath());
	}

	/**
	 * 判断一个目录是否是另外一个资源的前缀。<BR>
	 * @param folderName
	 * @param fileName
	 * @return
	 */
	public static boolean isPrefixOf(String folderName, String fileName) {
		if (null == folderName || null == fileName) {
			return false;
		}

		String t_FolderName = normalizeInUnixStyle(folderName);
		String t_ResourceName = normalizeInUnixStyle(fileName);

		return t_ResourceName.startsWith(t_FolderName);
	}

	/**
	 * Verifies that the specified filename represents an absolute path. Differs
	 * from new java.io.File("filename").isAbsolute() in that a path beginning
	 * with a double file separator--signifying a Windows UNC--must at minimum
	 * match "\\a\b" to be considered an absolute path.
	 * 
	 * @param filename
	 *            the filename to be checked.
	 * @return true if the filename represents an absolute path.
	 * @throws java.lang.NullPointerException
	 *             if filename is null.
	 * @since Ant 1.6.3
	 */
	public static boolean isAbsolutePath(String filename) {
		int len = filename.length();
		if (len == 0) {
			return false;
		}
		char sep = File.separatorChar;
		filename = filename.replace('/', sep).replace('\\', sep);
		char c = filename.charAt(0);
		if (!(onDos || onNetWare)) {
			return (c == sep);
		}
		if (c == sep) {
			if (!(onDos && len > 4 && filename.charAt(1) == sep)) {
				return false;
			}
			int nextsep = filename.indexOf(sep, 2);
			return nextsep > 2 && nextsep + 1 < len;
		}
		int colon = filename.indexOf(':');
		return (Character.isLetter(c) && colon == 1 && filename.length() > 2 && filename.charAt(2) == sep) || (onNetWare && colon > 0);
	}

}
