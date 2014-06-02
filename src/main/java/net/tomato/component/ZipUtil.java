package net.tomato.component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.EmptyFileFilter;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Mkdir;
import org.apache.tools.ant.taskdefs.Move;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;


/**
 * 压缩解压工具类<BR>
 */

public final class ZipUtil {

	public ZipUtil() {
		super();
	}

	/**
	 * 压缩文件（或者目录）
	 * @param inputFile
	 * @param outputFile
	 * @throws IOException
	 */
	public static void zip(File inputFile, File outputFile) throws IOException {
		ZipOutputStream output = null;
		try {
			output = new ZipOutputStream(new FileOutputStream(outputFile));
			if (inputFile.isDirectory()) {
				String basePath = inputFile.getParent();
				Collection<?> files = FileUtils.listFiles(inputFile, EmptyFileFilter.EMPTY, EmptyFileFilter.EMPTY);

				Iterator<?> iterator = files.iterator();
				while (iterator.hasNext()) {
					File file = (File) iterator.next();
					String relativePath = FilenameUtil.getRelativePath(basePath, file.getAbsolutePath());
					if (relativePath.charAt(0) == '\\' || relativePath.charAt(0) == '/') {
						relativePath = relativePath.substring(1);
					}
					FileInputStream input = new FileInputStream(file);
					output.putNextEntry(new ZipEntry(relativePath));

					try {
						IOUtils.copy(input, output);
					} catch (Exception e) {
						// Nothing to do
					} finally {
						IOUtils.closeQuietly(input);
					}
				}
			}
			else {
				FileInputStream input = new FileInputStream(inputFile);
				output.putNextEntry(new ZipEntry(inputFile.getName()));
				IOUtils.copy(input, output);
				IOUtils.closeQuietly(input);
			}
		} finally {
			IOUtils.closeQuietly(output);
		}
	}

	/**
	 * 解压zip文件到指定目录
	 * @param zipFilename
	 * @param outputDirectory
	 * @throws IOException
	 */
	public static void unzip(File zipFilename, File outputDirectory) throws IOException {
//		unzip2Dir(zipFilename.getAbsolutePath(), outputDirectory.getAbsolutePath(), null, null);  //在Linux线程模式下有内存溢出错误
		try {
		//	EOSZipper.unZip(zipFilename.getAbsolutePath(), outputDirectory.getAbsolutePath(), false);
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		}
	}

	/**
	 * 将zip压缩文件中的指定pattern的文件解压到某个目录
	 *
	 * @param zipfilepath		zip文件路径
	 * @param toDir				解压到的目录
	 * @param includesPattern	包含文件的pattern， 如 WEB-INF/*.xml
	 * @param excludsPattern	排除文件的pattern， 如 WEB-INF/*.xml
	 * @deprecated 在Linux下有内存溢出bug，不推荐使用
	 */
	@Deprecated
	public static void unzip2Dir(String zipfilepath, String toDir, String includesPattern, String excludsPattern) {
		unzip2Dir(zipfilepath, toDir, includesPattern, excludsPattern, true, true);

	}

	/** 将zip压缩文件中的指定pattern的文件解压到某个目录
	 *
	 * @param zipfilepath		zip文件路径
	 * @param toDir				解压到的目录
	 * @param includesPattern	包含文件的pattern， 如 WEB-INF/*.xml
	 * @param excludsPattern	排除文件的pattern， 如 WEB-INF/*.xml
	 * @param keepDir			是否保存文件的目录结构，如果不保存，则所有文件都解压到一个目录中
	 * @param overwrite			是否强行覆盖文件，如果不强行覆盖文件，则只有旧同名文件时间戳小于新同名文件时才会被新文件覆盖
	 *
	 * @deprecated 在Linux下有内存溢出bug，不推荐使用
	 */
	@Deprecated
	public static void unzip2Dir(String zipfilepath,
			String toDir,
			String includesPattern,
			String excludsPattern,
			boolean keepDir,
			boolean overwrite) {

		File destDir = new File(toDir);
		if (!destDir.exists())
			mkdir(destDir);

		Project prj = new Project();
		Expand expand = new Expand();
		expand.setProject(prj);
		expand.setSrc(new File(zipfilepath));
		expand.setOverwrite(overwrite);

		PatternSet patternset = new PatternSet();
		boolean hasPattern = false;
		if (StringUtil.isNotNull(includesPattern)) {
			patternset.setIncludes(includesPattern);
			patternset.setProject(prj);
			hasPattern = true;
		}
		if (StringUtil.isNotNull(excludsPattern)) {
			patternset.setExcludes(excludsPattern);
			patternset.setProject(prj);
			hasPattern = true;
		}
		if (hasPattern)
			expand.addPatternset(patternset);

		if (!keepDir) {
			/**
			 * for ant 1.7.0 FileNameMapper mapper=new FlatFileNameMapper();
			 * expand.add(mapper);
			 */

			/** for ant 1.6.5 * */
			File tmpDir = new File(toDir + "/" + new Date().getTime());
			expand.setDest(tmpDir);
			expand.execute();

			try {
				Move move = new Move();
				move.setProject(prj);
				move.setTodir(destDir);
				move.setFlatten(true);
				FileSet fs = new FileSet();
				fs.setProject(prj);
				fs.setDir(tmpDir);
				if (includesPattern != null)
					fs.setIncludes(includesPattern);
				if (excludsPattern != null)
					fs.setExcludes(excludsPattern);
				move.addFileset(fs);
				move.execute();
			} catch (Exception e) {
				// ignore error when nothing expanded
			}
			rmdirCascade(tmpDir);
		} else {
			expand.setDest(destDir);
			expand.execute();
		}
	}

	/**
	 * Unconditionally close an zip file.<BR>
	 *
	 * @param zipFile
	 */
	public static void closeQuietly(ZipFile zipFile) {
		if (zipFile != null) {
			try {
				zipFile.close();
			} catch (Exception ignored) {
				//Nothing to do
			}
		}
	}

	/**
	 * 压缩文件中的文件个数
	 * @param zipFilename
	 * @return
	 */
	public static int countOfZip(File zipFilename) {
		ZipFile zipFile;
		try {
			int size = 0;
			zipFile = new ZipFile(zipFilename);
			Enumeration<?> en = zipFile.entries();
			ZipEntry zipEntry = null;
			while (en.hasMoreElements()) {
				zipEntry = (ZipEntry) en.nextElement();
				if (!zipEntry.isDirectory()) {
					size++;
				}
			}
			zipFile.close();
			return size;
		} catch (IOException e) {
			return 0;
		}
	}

	/**
	 * 增加文件到zip文件中
	 *
	 * @param zipFilepath zip文件路径，如果不存在，则新建
	 * @param srcDir 要添加的文件目录
	 * @param includesPattern 包含文件的pattern， 如 WEB-INF/*.xml
	 * @param excludsPattern 排除文件的pattern， 如 WEB-INF/*.xml
	 */
	public static void addFilesToZip(String zipFilepath,
			String srcDir,
			String includesPattern,
			String excludsPattern) throws FileNotFoundException {

		if (!(new File(srcDir).exists()))
			throw new FileNotFoundException(srcDir + " can not be found!");

		Project prj = new Project();
		Zip zip = new Zip();
		zip.setProject(prj);
		zip.setDestFile(new File(zipFilepath));
		zip.setUpdate(true);
		FileSet fileSet = new FileSet();
		fileSet.setProject(prj);
		fileSet.setDir(new File(srcDir));
		if (StringUtil.isNotNull(includesPattern)) {
			fileSet.setIncludes(includesPattern);
		}

		if (StringUtil.isNotNull(excludsPattern)) {
			fileSet.setExcludes(excludsPattern);
		}

		zip.addFileset(fileSet);
		zip.execute();
	}

	/** 创建一个目录
	 *
	 * @param dir
	 */
	private static void mkdir(File dir) {
		Project prj=new Project();
		Mkdir mkdir=new Mkdir();
		mkdir.setProject(prj);
		mkdir.setDir(dir);
		mkdir.execute();
	}

	/** 级联删除一个目录，包括子目录和文件
	 *
	 * @param dirpath
	 */
	private static void rmdirCascade(File dir) {
		if(!dir.exists())
			return;
		Project prj=new Project();
		Delete delete=new Delete();
		delete.setProject(prj);
		delete.setDir(dir);
		delete.execute();
	}
}
