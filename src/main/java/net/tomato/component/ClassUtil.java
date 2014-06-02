package net.tomato.component;


import java.beans.Introspector;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.ConstructorUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.ObjectUtils;

/**
 * 类的辅助类<BR>
 * 主要是用来帮助编写一些反射程序用的 <BR>
 */

public final class ClassUtil {

	/** Suffix for array class names */
	public static final String ARRAY_SUFFIX = "[]";

	/**
	 * Map with primitive wrapper type as key and corresponding primitive type
	 * as value, for example: Integer.class -> int.class.
	 */
	private static final Map<Class, Class> primitiveWrapperTypeMap = new HashMap<Class, Class>(8);

	/**
	 * Map with primitive type name as key and corresponding primitive type as
	 * value, for example: "int" -> "int.class".
	 */
	private static final Map<String, Class> primitiveTypeNameMap = new HashMap<String, Class>(8);

	/**
     * Maps a primitive class name to its corresponding abbreviation used in array class names.
     */
    private static Map abbreviationMap = new HashMap();

    static {
        abbreviationMap.put( "int", "I" );
        abbreviationMap.put( "boolean", "Z" );
        abbreviationMap.put( "float", "F" );
        abbreviationMap.put( "long", "J" );
        abbreviationMap.put( "short", "S" );
        abbreviationMap.put( "byte", "B" );
        abbreviationMap.put( "double", "D" );
        abbreviationMap.put( "char", "C" );
    }

	static {
		primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
		primitiveWrapperTypeMap.put(Byte.class, byte.class);
		primitiveWrapperTypeMap.put(Character.class, char.class);
		primitiveWrapperTypeMap.put(Double.class, double.class);
		primitiveWrapperTypeMap.put(Float.class, float.class);
		primitiveWrapperTypeMap.put(Integer.class, int.class);
		primitiveWrapperTypeMap.put(Long.class, long.class);
		primitiveWrapperTypeMap.put(Short.class, short.class);

		for (Iterator iterator = primitiveWrapperTypeMap.values().iterator(); iterator.hasNext();) {
			Class primitiveClass = (Class) iterator.next();
			primitiveTypeNameMap.put(primitiveClass.getName(), primitiveClass);
		}
	}

	/**
	 * 因为不需要实例，所以构造函数为私有<BR>
	 * 参见Singleton模式<BR>
	 *
	 * Only one instance is needed,so the default constructor is private<BR>
	 * Please refer to singleton design pattern.
	 */
	private ClassUtil() {
		super();
	}

	/**
	 * Return a default ClassLoader to use (never <code>null</code>). Returns
	 * the thread context ClassLoader, if available. The ClassLoader that loaded
	 * the ClassUtils class will be used as fallback.
	 * <p>
	 * Call this method if you intend to use the thread context ClassLoader in a
	 * scenario where you absolutely need a non-null ClassLoader reference: for
	 * example, for class path resource loading (but not necessarily for
	 * <code>Class.forName</code>, which accepts a <code>null</code>
	 * ClassLoader reference as well).
	 *
	 * @see java.lang.Thread#getContextClassLoader()
	 */
	public static ClassLoader getDefaultClassLoader() {
		ClassLoader classLoader = null;
		try {
			classLoader = Thread.currentThread().getContextClassLoader();
		} catch (Throwable ignore) {
			//Nothing to do
		}
		if (classLoader == null) {
			// No thread context class loader -> use class loader of this class.
			classLoader = ClassUtil.class.getClassLoader();
		}
		return classLoader;
	}

	private static Class getClass(ClassLoader classLoader, String className, boolean initialize) throws ClassNotFoundException {
		Class clazz;
		if (abbreviationMap.containsKey(className)) {
			String clsName = "[" + abbreviationMap.get(className);
			clazz = Class.forName(clsName, initialize, classLoader).getComponentType();
		} else {
			clazz = Class.forName(toProperClassName(className), initialize, classLoader);
		}
		return clazz;
	}

	private static String toProperClassName(String className) {
		className = StringUtil.deleteWhitespace(className);
		if (className == null) {
			throw new NullArgumentException("className");
		}
		if (className.endsWith("[]")) {
			StringBuffer classNameBuffer = new StringBuffer();
			for (; className.endsWith("[]"); classNameBuffer.append("[")) {
				className = className.substring(0, className.length() - 2);
			}

			String abbreviation = (String) abbreviationMap.get(className);
			if (abbreviation != null) {
				classNameBuffer.append(abbreviation);
			} else {
				classNameBuffer.append("L").append(className).append(";");
			}
			className = classNameBuffer.toString();
		}
		return className;
	}

	/**
	 * 查找某一个类
	 *
	 * @param classLoader
	 * @param className
	 * @return
	 * @throws ClassNotFoundException
	 */
	public static Class getClass(ClassLoader classLoader, String className)
			throws ClassNotFoundException {
		return getClass(classLoader, className, true);
	}

	/**
	 * 创建一个ClassLoader，这样可以从指定的库文件(jar或者是zip文件)中载入新的类<BR>
	 *
	 * Create a ClassLoader to load new classes from the assigned file(jar or zip)<BR>
	 *
	 * @param fileNames
	 *            Java library file names.
	 *
	 * @return return a class loader which can load class from the specified java libraries.
	 *
	 * @throws IOException
	 */
	public static URLClassLoader loadLibrary(final String[] fileNames) throws IOException {
		final File[] files = new File[fileNames.length];

		for (int i = 0; i < files.length; i++) {
			files[i] = new File(fileNames[i]);
		}

		URL[] urls = FileUtils.toURLs(files);
		return URLClassLoader.newInstance(urls);
	}

	/**
	 * Return whether the {@link Class} identified by the supplied name is
	 * present and can be loaded. Will return <code>false</code> if either the
	 * class or one of its dependencies is not present or cannot be loaded.
	 */
	public static boolean isPresent(String className) {
		try {
			getClass(getDefaultClassLoader(), className);
			return true;
		} catch (Throwable ex) {
			return false;
		}
	}

	/**
	 * Return whether the {@link Class} identified by the supplied name is
	 * present and can be loaded. Will return <code>false</code> if either the
	 * class or one of its dependencies is not present or cannot be loaded.
	 *
	 * @param classLoader
	 */
	public static boolean isPresent(String className, ClassLoader classLoader) {
		try {
			getClass(classLoader, className);
			return true;
		} catch (Throwable ex) {
			return false;
		}
	}

	/**
	 * Resolve the given class name as primitive class, if appropriate.
	 *
	 * @param name
	 *            the name of the potentially primitive class
	 * @return the primitive class, or <code>null</code> if the name does not
	 *         denote a primitive class
	 */
	public static Class resolvePrimitiveClassName(String name) {
		Class result = null;
		// Most class names will be quite long, considering that they
		// SHOULD sit in a package, so a length check is worthwhile.
		if ((name != null) && (name.length() <= 8)) {
			// Could be a primitive - likely.
			result = (Class) primitiveTypeNameMap.get(name);
		}
		return result;
	}

	/**
	 * Return the short string name of a Java class in decapitalized JavaBeans
	 * property format.
	 *
	 * @param clazz
	 *            the class
	 * @return the short name rendered in a standard JavaBeans property format
	 * @see java.beans.Introspector#decapitalize(String)
	 */
	public static String getShortNameAsProperty(Class clazz) {
		return Introspector.decapitalize(ClassUtils.getShortClassName(clazz));
	}

	/**
	 * Return the qualified name of the given class: usually simply the class
	 * name, but component type class name + "[]" for arrays.
	 *
	 * @param clazz
	 *            the class
	 * @return the qualified name of the class
	 */
	public static String getQualifiedName(Class clazz) {
		if (clazz.isArray()) {
			return getQualifiedNameForArray(clazz);
		}
		else {
			return clazz.getName();
		}
	}

	/**
	 * Build a nice qualified name for an array: component type class name +
	 * "[]".
	 *
	 * @param clazz
	 *            the array class
	 * @return a qualified name for the array class
	 */
	private static String getQualifiedNameForArray(Class clazz) {
		StringBuffer buffer = new StringBuffer();
		while (clazz.isArray()) {
			clazz = clazz.getComponentType();
			buffer.append(ClassUtil.ARRAY_SUFFIX);
		}
		buffer.insert(0, clazz.getName());
		return buffer.toString();
	}

	/**
	 * Return the qualified name of the given method, consisting of fully
	 * qualified interface/class name + "." + method name.
	 *
	 * @param method
	 *            the method
	 * @return the qualified name of the method
	 */
	public static String getQualifiedMethodName(Method method) {
		return method.getDeclaringClass().getName() + "." + method.getName();
	}

	/**
	 * Does the given class and/or its superclasses at least have one or more
	 * methods (with any argument types)? Includes non-public methods.
	 *
	 * @param clazz
	 *            the clazz to check
	 * @param methodName
	 *            the name of the method
	 * @return whether there is at least one method with the given name
	 */
	public static boolean hasMethod(Class clazz, String methodName) {
		for (int i = 0; i < clazz.getDeclaredMethods().length; i++) {
			Method method = clazz.getDeclaredMethods()[i];
			if (method.getName().equals(methodName)) {
				return true;
			}
		}
		Class[] interfaces = clazz.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			if (hasMethod(interfaces[i], methodName)) {
				return true;
			}
		}
		if (clazz.getSuperclass() != null) {
			return hasMethod(clazz.getSuperclass(), methodName);
		}
		return false;
	}

	/**
	 * Determine whether the given class has a method with the given signature.
	 * <p>
	 * Essentially translates <code>NoSuchMethodException</code> to "false".
	 *
	 * @param clazz
	 *            the clazz to analyze
	 * @param methodName
	 *            the name of the method
	 * @param argumentTypes
	 *            the parameter types of the method
	 * @see java.lang.Class#getMethod
	 */
	public static boolean hasMethod(Class clazz, String methodName, Class[] argumentTypes) {
		return (getMethod(clazz, methodName, argumentTypes) != null);
	}

	/**
	 * Return the number of methods with a given name (with any argument types),
	 * for the given class and/or its superclasses. Includes non-public methods.
	 *
	 * @param clazz
	 *            the clazz to check
	 * @param methodName
	 *            the name of the method
	 * @return the number of methods with the given name
	 */
	public static int getMethodCountForName(Class clazz, String methodName) {
		int count = 0;
		for (int i = 0; i < clazz.getDeclaredMethods().length; i++) {
			Method method = clazz.getDeclaredMethods()[i];
			if (methodName.equals(method.getName())) {
				count++;
			}
		}
		Class[] interfaces = clazz.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			count += getMethodCountForName(interfaces[i], methodName);
		}
		if (clazz.getSuperclass() != null) {
			count += getMethodCountForName(clazz.getSuperclass(), methodName);
		}
		return count;
	}

	/**
	 * Return a static method of a class.
	 *
	 * @param methodName
	 *            the static method name
	 * @param clazz
	 *            the class which defines the method
	 * @param args
	 *            the parameter types to the method
	 * @return the static method, or <code>null</code> if no static method was
	 *         found
	 * @throws IllegalArgumentException
	 *             if the method name is blank or the clazz is null
	 */
	public static Method getStaticMethod(Class clazz, String methodName, Class[] args) {
		try {
			Method method = clazz.getDeclaredMethod(methodName, args);
			if ((method.getModifiers() & Modifier.STATIC) != 0) {
				return method;
			}
		} catch (NoSuchMethodException ex) {
			//Nothing to do
		}
		return null;
	}

	/**
	 * Check if the given class represents a primitive wrapper, i.e. Boolean,
	 * Byte, Character, Short, Integer, Long, Float, or Double.
	 */
	public static boolean isPrimitiveWrapper(Class clazz) {
		return primitiveWrapperTypeMap.containsKey(clazz);
	}

	/**
	 * Check if the given class represents a primitive (i.e. boolean, byte,
	 * char, short, int, long, float, or double) or a primitive wrapper (i.e.
	 * Boolean, Byte, Character, Short, Integer, Long, Float, or Double).
	 */
	public static boolean isPrimitiveOrWrapper(Class clazz) {
		return (clazz.isPrimitive() || isPrimitiveWrapper(clazz));
	}

	/**
	 * Check if the given class represents an array of primitives, i.e. boolean,
	 * byte, char, short, int, long, float, or double.
	 */
	public static boolean isPrimitiveArray(Class clazz) {
		return (clazz.isArray() && getRealComponentType(clazz).isPrimitive());
	}

	/**
	 * Check if the given class represents an array of primitive wrappers, i.e.
	 * Boolean, Byte, Character, Short, Integer, Long, Float, or Double.
	 */
	public static boolean isPrimitiveWrapperArray(Class clazz) {
		return (clazz.isArray() && isPrimitiveWrapper(getRealComponentType(clazz)));
	}

	/**
	 * Recusive to get the real component class.<BR>
	 * For example: java.lang.String[][][][] will return java.lang.String instead of java.lang.String[][][].<BR>
	 *
	 * @param clazz If the parameter is <code>null</code>,just return <code>null</code>.
	 * @return
	 */
	public static Class getRealComponentType(Class clazz) {
		if (clazz.isArray()) {
			Class newClazz = clazz.getComponentType();
			return getRealComponentType(newClazz);
			//Make a recusion.
		}
		else {
			return clazz;
		}
	}

	/**
	 * Determine if the given target type is assignable from the given value
	 * type, assuming setting by reflection. Considers primitive wrapper
	 * classes as assignable to the corresponding primitive types.
	 * @param targetType the target type
	 * @param valueType	the value type that should be assigned to the target type
	 * @return if the target type is assignable from the value type
	 */
	public static boolean isAssignable(Class targetType, Class valueType) {
		return (targetType.isAssignableFrom(valueType) ||
				targetType.equals(primitiveWrapperTypeMap.get(valueType)));
	}

	/**
	 * Determine if the given type is assignable from the given value, assuming
	 * setting by reflection. Considers primitive wrapper classes as assignable
	 * to the corresponding primitive types.
	 *
	 * @param type
	 *            the target type
	 * @param value
	 *            the value that should be assigned to the type
	 * @return if the type is assignable from the value
	 */
	public static boolean isAssignableValue(Class type, Object value) {
		return (value != null ? isAssignable(type, value.getClass()) : !type.isPrimitive());
	}

	/**
	 * Return a path suitable for use with <code>ClassLoader.getResource</code>
	 * (also suitable for use with <code>Class.getResource</code> by
	 * prepending a slash ('/') to the return value. Built by taking the package
	 * of the specified class file, converting all dots ('.') to slashes ('/'),
	 * adding a trailing slash if necesssary, and concatenating the specified
	 * resource name to this. <br/>As such, this function may be used to build a
	 * path suitable for loading a resource file that is in the same package as
	 * a class file, although
	 * {@link org.springframework.core.io.ClassPathResource} is usually even
	 * more convenient.
	 *
	 * @param clazz
	 *            the Class whose package will be used as the base
	 * @param resourceName
	 *            the resource name to append. A leading slash is optional.
	 * @return the built-up resource path
	 * @see java.lang.ClassLoader#getResource
	 * @see java.lang.Class#getResource
	 */
	public static String addResourcePathToPackagePath(Class clazz, String resourceName) {
		if (!resourceName.startsWith("/")) {
			return classPackageAsResourcePath(clazz) + "/" + resourceName;
		}
		return classPackageAsResourcePath(clazz) + resourceName;
	}

	/**
	 * Given an input class object, return a string which consists of the
	 * class's package name as a pathname, i.e., all dots ('.') are replaced by
	 * slashes ('/'). Neither a leading nor trailing slash is added. The result
	 * could be concatenated with a slash and the name of a resource, and fed
	 * directly to ClassLoader.getResource(). For it to be fed to
	 * Class.getResource, a leading slash would also have to be prepended to the
	 * return value.
	 *
	 * @param clazz
	 *            the input class. A null value or the default (empty) package
	 *            will result in an empty string ("") being returned.
	 * @return a path which represents the package name
	 * @see ClassLoader#getResource
	 * @see Class#getResource
	 */
	public static String classPackageAsResourcePath(Class clazz) {
		if ((clazz == null) || (clazz.getPackage() == null)) {
			return "";
		}
		return clazz.getPackage().getName().replace('.', '/');
	}

	/**
	 * Return all interfaces that the given object implements as array,
	 * including ones implemented by superclasses.
	 *
	 * @param object
	 *            the object to analyse for interfaces
	 * @return all interfaces that the given object implements as array
	 */
	public static Class[] getAllInterfaces(Object object) {
		Set interfaces = getAllInterfacesAsSet(object);
		return (Class[]) interfaces.toArray(new Class[interfaces.size()]);
	}

	/**
	 * Return all interfaces that the given class implements as array, including
	 * ones implemented by superclasses.
	 * <p>
	 * If the class itself is an interface, it gets returned as sole interface.
	 *
	 * @param clazz
	 *            the class to analyse for interfaces
	 * @return all interfaces that the given object implements as array
	 */
	public static Class[] getAllInterfacesForClass(Class clazz) {
		Set interfaces = getAllInterfacesForClassAsSet(clazz);
		return (Class[]) interfaces.toArray(new Class[interfaces.size()]);
	}

	/**
	 * Return all interfaces that the given object implements as List, including
	 * ones implemented by superclasses.
	 *
	 * @param object
	 *            the object to analyse for interfaces
	 * @return all interfaces that the given object implements as List
	 */
	public static Set getAllInterfacesAsSet(Object object) {
		return getAllInterfacesForClassAsSet(object.getClass());
	}

	/**
	 * Return all interfaces that the given class implements as Set, including
	 * ones implemented by superclasses.
	 * <p>
	 * If the class itself is an interface, it gets returned as sole interface.
	 *
	 * @param clazz
	 *            the class to analyse for interfaces
	 * @return all interfaces that the given object implements as Set
	 */
	public static Set getAllInterfacesForClassAsSet(Class clazz) {
		return new HashSet(ClassUtils.getAllInterfaces(clazz));
	}

	/**
	 * 反射调用方法
	 *
	 * @param clazz
	 *            反射类
	 * @param methodName
	 *            方法名
	 * @param args
	 *            参数
	 * @return 调用结果
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 */
	public static Object invokeMethod(Class clazz, String methodName, Object[] args) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Object target = clazz.newInstance();
		return getMethod(clazz, methodName, args).invoke(target, args);
	}

	/**
	 * 反射调用方法
	 *
	 * @param clazz
	 *            反射类
	 * @param methodName
	 *            方法名
	 * @param args
	 *            参数
	 * @return 调用结果
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 */
//	@REVIEW 如果没有找到方法，不应该直接抛出nullpointerexception.[wangfeng]{wuyh}
	public static Object invokeMethod(Class clazz, String methodName, Object[] args, boolean allowSuperType) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Object target = clazz.newInstance();
		return getMethod(clazz, methodName, args).invoke(target, args);
	}

	/**
	 * 反射调用方法
	 *
	 * @param clazzName 反射类名
	 * @param methodName 方法名
	 * @param args 参数
	 * @return 调用结果
	 * @throws Throwable
	 */
//	@REVIEW 如果没有找到方法，不应该直接抛出nullpointerexception.[wangfeng]{wuyh}
	public static Object invokeMethod(String clazzName, String methodName, Object[] args) throws Throwable {
		Class<?> clazz = getClass(getDefaultClassLoader(), clazzName);
		Object target = null;
		try {
			target = clazz.newInstance();
		} catch (Exception ignore) {

		}
		return getMethod(clazz, methodName, args).invoke(target, args);
	}

	/**
	 * 反射调用方法
	 *
	 * @param object 对象
	 * @param methodName 方法名
	 * @param types 类型
	 * @param args 参数
	 * @return 调用结果
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 */
//	@REVIEW 如果没有找到方法，不应该直接抛出nullpointerexception.[wangfeng]{wuyh}
	public static Object invokeMethod(Object object, String methodName, Class[] types, Object[] args)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return getMethod(object.getClass(), methodName, types).invoke(object, args);
	}

	/**
	 * 反射调用方法
	 *
	 * @param object 对象
	 * @param methodName 方法名
	 * @param args 参数
	 * @return 调用结果
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 */
//	@REVIEW 如果没有找到方法，不应该直接抛出nullpointerexception.[wangfeng]{wuyh}
	public static Object invokeMethod(Object object, String methodName, Object[] args) throws InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return getMethod(object.getClass(), methodName, args).invoke(object, args);
	}

	/**
	 * 使用预先指定的对象找到相应的构造函数<BR>
	 * 再使用传入的对象构造指定类的实例<BR>
	 * 如果传入的类名为空<BR>
	 * 抛出IllegalArgumentException异常<BR>
	 *
	 * Create a class with the parameters.<BR>
	 * It will use the constructor with the assigned classes of objects.<BR>
	 * and then create object with the assigned objects<BR>
	 * If the class name is null,a IllegalArgumentException will be thrown.
	 *
	 * @param className
	 *            the class name for creating a new object.
	 * @param objects
	 *            the parameter values for creating a new object.
	 * @throws LinkageError
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws NoSuchMethodException
	 */
	public static Object newInstance(final String className, final Object[] objects) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, LinkageError {
		if (StringUtil.isBlank(className)) {
			return new IllegalArgumentException("the class name can't be null");
		}

		Class clazz = getClass(getDefaultClassLoader(), className);
		return ConstructorUtils.invokeConstructor(clazz, objects);
	}

	/**
	 * 判断一个方法是否是静态方法。<BR>
	 *
	 * @param targetClassName
	 * @param methodName
	 * @param argumentTypes
	 * @param args
	 * @return
	 * @throws ClassNotFoundException
	 */
	public static boolean isStaticMethod(String targetClassName, String methodName, Object[] args) throws ClassNotFoundException {
		return isStaticMethod(Class.forName(targetClassName), methodName, args);
	}

	/**
	 * 判断一个方法是否是静态方法。<BR>
	 *
	 * @param targetClassName
	 * @param methodName
	 * @param argumentTypes
	 * @param args
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static boolean isStaticMethod(Class targetClass, String methodName, Object[] args) throws IllegalArgumentException {

		Class[] classes = getClasses(args);
		return isStaticMethod(targetClass, methodName, classes);
	}

	/**
	 * 将一个Object数组转成Class数组。<BR>
	 * 返回数组的值中存在 <code>null</code>元素。<BR>
	 *
	 * @param args
	 * @return
	 */
//	@REVIEW 在查找方法中用到，如为参数为Null的，抛出异常不合理.[wangfeng]{wuyh}
	public static Class[] getClasses(Object[] args) {
		if (ArrayUtils.isEmpty(args)) {
			return ArrayUtil.NULL_CLASSES;
		}

		Class[] classes = new Class[args.length];
		for (int i = 0; i < args.length; i++) {
			Object object = args[i];
			if (null == object) {
				throw new IllegalArgumentException("the object array should not contains null");
			}
			else {
				classes[i] = object.getClass();
				if (primitiveWrapperTypeMap.containsKey(classes[i])) {
					classes[i] = primitiveWrapperTypeMap.get(classes[i]);
				}
			}
		}

		return classes;
	}

	/**
	 * 判断一个方法是否是静态方法
	 *
	 * @param targetClass
	 * @param methodName
	 * @param argumentTypes
	 * @param args
	 * @return
	 */
//	@REVIEW 如果没有找到方法，增加决断.[wangfeng]{wuyh}
	public static boolean isStaticMethod(Class targetClass, String methodName, Class[] argumentTypes) {
		Method method = getMethod(targetClass, methodName, argumentTypes);
		return Modifier.isStatic(method.getModifiers());
	}

	/**
	 * 返回指定名称和参数的方法。<BR>
	 *
	 * Return a method with the specified name and parameters.<BR>
	 *
	 * @param clazz
	 * @param name
	 * @param parameterTypes
	 * @return
	 */
	public static Method getMethod(Class clazz, String name, Object[] args) {
		return getMethod(clazz, name, args, true);
	}

	/**
	 * 返回指定名称和参数的方法。<BR>
	 *
	 * Return a method with the specified name and parameters.<BR>
	 *
	 * @param clazz
	 * @param name
	 * @param parameterTypes
	 * @return
	 */
	public static Method getMethod(Class clazz, String name, Object[] args, boolean allowSuperType) {
		Class[] classes = getClasses(args);
		return getMethod(clazz, name, classes, allowSuperType);
	}

	/**
	 * 返回指定名称和参数的方法。<BR>
	 *
	 * Return a method with the specified name and parameters.<BR>
	 *
	 * @param clazz
	 * @param name
	 * @param parameterTypes
	 * @return
	 */
	public static Method getMethod(Class clazz, String name, Class[] parameterTypes) {
		return getMethod(clazz, name, parameterTypes, true);
	}

	/**
	 * 返回指定名称和参数的方法。<BR>
	 *
	 * Return a method with the specified name and parameters.<BR>
	 *
	 * @param clazz
	 * @param name
	 * @param parameterTypes
	 * @param allowSuperType 有时候，子类是可以赋给超类的，如果允许该检查，请将该参数设为 <code>true</code>。
	 * @return
	 */
	public static Method getMethod(Class clazz, String name, Class[] parameterTypes, boolean allowSuperType) {
		Method[] methods = clazz.getMethods();
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			if (method.getName().equals(name)) {
				if (isParameterEqual(method, parameterTypes, allowSuperType)) {
					return method;
				}
			}
		}
		methods = clazz.getDeclaredMethods();
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			if (method.getName().equals(name)) {
				if (isParameterEqual(method, parameterTypes, allowSuperType)) {
					return method;
				}
			}
		}
		return null;
	}

	/**
	 * 判断指定的类是否与指定的方法所声明的参数一致。<BR>
	 *
	 * @param method
	 * @param parameterTypes
	 * @param allowSuperType 有时候，子类是可以赋给超类的，如果允许该检查，请将该参数设为 <code>true</code>。
	 * @return
	 */
	public static boolean isParameterEqual(Method method, Class[] parameterTypes, boolean allowSuperType) {
		if (null == method) {
			return false;
		}
		Class[] methodParameterTypes = method.getParameterTypes();

		if (ArrayUtils.isEmpty(parameterTypes) && ArrayUtils.isEmpty(methodParameterTypes)) {
			return true;
		}

		if (ArrayUtils.getLength(parameterTypes) != ArrayUtils.getLength(methodParameterTypes)) {
			return false;
		}

		if (allowSuperType) {
			return ClassUtils.isAssignable(parameterTypes, methodParameterTypes);
		}
		else {
			return Arrays.equals(parameterTypes, methodParameterTypes);
		}
	}

	/**
	 * 取得指定对象中某个字段的值。<BR>
	 *
	 * Get the value of the specified field.<BR>
	 *
	 * @param object
	 * @param fieldName
	 * @return
	 * @throws NoSuchFieldException
	 */
	public static Object getFieldValue(Object object, String fieldName) throws NoSuchFieldException {

		try {
			if (null == object) {
				return null;
			}

			Field field = object.getClass().getDeclaredField(fieldName);

			if (null == field) {
				field = object.getClass().getField(fieldName);
			}

			if (null != field) {
				field.setAccessible(true);
				return field.get(object);
			}
			else {
				return null;
			}

		} catch (Exception e) {
			if (e instanceof NoSuchFieldException) {
				try {
					//如果不存在 Field, 将查找所有父类的 Field, 依然不存在, 将 throw 异常
					return getNotAccessibleFieldValue(object, fieldName);
				} catch (Exception ex1) {
					//Nothing to do
				}
			}
		}

		throw new NoSuchFieldException("The field of " + fieldName + "not founded.");
	}

	/**
	 * 的到Method的方法签名
	 * @param method
	 * @return
	 */
	public static String getMethodSingnature(Method method) {
		StringBuffer buffer = new StringBuffer();
		String methodName = method.getName();
		buffer.append(methodName);
		buffer.append("(");
		Class[] argumentTypes = method.getParameterTypes();
		for (int i = 0; i < argumentTypes.length; i++) {
			buffer.append(argumentTypes[i].getSimpleName());
			if (i < (argumentTypes.length - 1)) {
				buffer.append(", ");
			}
		}
		buffer.append(")");
		return buffer.toString();
	}

	/**
	 * 得到没有公开的字段值，是通过setAccessible来访问的。<BR>
	 * 如果指定对象的类中没有声明，就会递归的去父类中查找。<BR>
	 *
	 * @param object
	 * @param fieldName
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private static Object getNotAccessibleFieldValue(Object object, String fieldName) throws IllegalArgumentException, IllegalAccessException {
		Field field = null;
		Class superClass = object.getClass().getSuperclass();
		while (true) {
			if (superClass == null) {
				return null;
			}
			try {
				field = superClass.getDeclaredField(fieldName);
				break;
			} catch (NoSuchFieldException noSuchFieldException1) {
				superClass = superClass.getSuperclass();
			}
		}
		if (null != field) {
			field.setAccessible(true);
			return field.get(object);
		}
		else {
			return null;
		}
	}

	/**
	 * 将实参类型转换为字符
	 *
	 * @param args
	 *            实参（数组）
	 * @return '(实参类型1,实参类型2,...)'格式字符串
	 */
	public static String argumentTypesToString(Object[] args) {
		return "(" + Arrays.toString(args) + ")";
	}

	/**
	 * 检查源对象和目标对象，将源对象中与目标对象名称相同的field拷贝到目标对象中。<BR>
	 *
	 * Copy the values of the fields from the source object to the target object, which have the same name both in the
	 * source and target objects.
	 *
	 * @param sourceObject
	 *            the source object.
	 * @param targetObject
	 *            the target object.
	 */
	public static void copyFields(final Object sourceObject, final Object targetObject) {
		if ((null == sourceObject) || (null == targetObject)) {
			throw new NullPointerException("both of the objects should not be null.");
		}
		Map fieldMap = new HashMap();

		Field[] sourceFields = getAllFields(sourceObject.getClass(), Modifier.STATIC | Modifier.FINAL);
		for (int i = 0; i < sourceFields.length; i++) {
			Field field = sourceFields[i];
			fieldMap.put(field.getName(), field);
		}

		Field[] targetFields = getAllFields(sourceObject.getClass(), Modifier.STATIC | Modifier.FINAL);
		for (int i = 0; i < targetFields.length; i++) {
			Field targetField = targetFields[i];
			Field sourceField = (Field) fieldMap.get(targetField.getName());

			if (null != sourceField) {
				try {
					sourceField.setAccessible(true);
					targetField.setAccessible(true);
					Object value = sourceField.get(sourceObject);
					targetField.set(targetObject, value);
				} catch (Exception ignore) {
					// Nothing to do
				}
			}
		}
	}

	/**
	 * 在指定类及其父类中查询所有符合过滤条件的字段。<BR>
	 * 过滤条件是(field.getModifiers() & filer) == 0。<BR>
	 *
	 * Get all the fields in the specified class and the super classes by the filter condition.<BR>
	 * The filter condition is "(field.getModifiers() & filer) == 0".<BR>
	 *
	 * @param clazz
	 *            the target class to get fields.
	 * @param filter
	 *            the condition to filter fields.
	 *
	 */
	public static Field[] getAllFields(Class clazz, int filter) {
		List list = new ArrayList();

		while (null != clazz) {
			CollectionUtils.addAll(list, getFields(clazz, filter));
			clazz = clazz.getSuperclass();
		}

		Field[] resultFields = new Field[list.size()];
		list.toArray(resultFields);

		return resultFields;
	}

	/**
	 * 在指定类中查询所有符合过滤条件的字段。<BR>
	 * 过滤条件是(field.getModifiers() & filer) == 0。<BR>
	 *
	 * Get all the fields in the specified class by the filter condition.<BR>
	 * The filter condition is "(field.getModifiers() & filer) == 0".<BR>
	 *
	 * @param clazz
	 *            the target class to get fields.
	 * @param filter
	 *            the condition to filter fields.
	 *
	 */
	public static Field[] getFields(Class clazz, int filter) {
		List list = new ArrayList();

		Field[] fields = clazz.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];

			if ((field.getModifiers() & filter) == 0) {
				list.add(field);
			}
		}

		Field[] resultFields = new Field[list.size()];
		list.toArray(resultFields);

		return resultFields;
	}

	/**
	 * 用来比较两个对象是否相等，通过field字段进行比较。<BR>
	 * 如果所有的字段都相等，则两个对象相等，返回值为<code>true</code>。
	 *
	 * This function is used to compare the fields in 2 objects.<BR>
	 * If all the fields are equal.The result will be <code>true</code>.
	 *
	 *
	 * @param sourceObject
	 *            the source object for comparison
	 * @param targetObject
	 *            the target object for comparison
	 *
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	public static boolean isEqualOnFields(Object sourceObject, Object targetObject) throws IllegalArgumentException, IllegalAccessException {
		if ((null == sourceObject) && (null == targetObject)) {
			return true;
		}

		if ((null == sourceObject) || (null == targetObject)) {
			return false;
		}

		if (sourceObject.getClass() != targetObject.getClass()) {
			return false;
		}

		if (sourceObject instanceof Comparable) {
			return 0 == ((Comparable) sourceObject).compareTo(targetObject);
		}

		Field[] fields = getAllFields(sourceObject.getClass(), Modifier.STATIC | Modifier.FINAL);
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];

			field.setAccessible(true);

			Object sourceValue = field.get(sourceObject);
			Object targetValue = field.get(targetObject);

			if (field.getType().isArray()) {
				if (!ArrayUtils.isEquals(sourceValue, targetValue)) {
					return false;
				}
			}
			else {
				if (!ObjectUtils.equals(sourceValue, targetValue)) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * 返回指定名称的字段。<BR>
	 *
	 * Return a field with the specified name.<BR>
	 *
	 * @param clazz
	 * @param name
	 * @return
	 */
	public static Field getField(Class clazz, String name) {

		Field[] fields = clazz.getFields();
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];
			if (field.getName().equals(name)) {
				return field;
			}
		}
		fields = clazz.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];
			if (field.getName().equals(name)) {
				return field;
			}
		}
		return null;
	}

	/**
	 * Invoke the specified {@link Method} against the supplied target object
	 * with no arguments. The target object can be <code>null</code> when
	 * invoking a static {@link Method}.
	 * <p>Thrown exceptions are handled via a call to {@link #handleReflectionException}.
	 * @param method the method to invoke
	 * @param target the target object to invoke the method on
	 * @return the invocation result, if any
	 * @see #invokeMethod(java.lang.reflect.Method, Object, Object[])
	 */
	public static Object invokeMethod(Method method, Object target) {
		return invokeMethod(method, target, null);
	}

	/**
	 * Invoke the specified {@link Method} against the supplied target object
	 * with the supplied arguments. The target object can be <code>null</code>
	 * when invoking a static {@link Method}.
	 * <p>Thrown exceptions are handled via a call to {@link #handleReflectionException}.
	 * @param method the method to invoke
	 * @param target the target object to invoke the method on
	 * @param args the invocation arguments (may be <code>null</code>)
	 * @return the invocation result, if any
	 * @see #invokeMethod(java.lang.reflect.Method, Object, Object[])
	 */
	public static Object invokeMethod(Method method, Object target, Object[] args) {
		try {
			return method.invoke(target, args);
		}
		catch (IllegalAccessException ex) {
			handleReflectionException(ex);
			throw new IllegalStateException(
					"Unexpected reflection exception - " + ex.getClass().getName() + ": " + ex.getMessage());
		}
		catch (InvocationTargetException ex) {
			handleReflectionException(ex);
			throw new IllegalStateException(
					"Unexpected reflection exception - " + ex.getClass().getName() + ": " + ex.getMessage());
		}
	}

	/**
	 * Handle the given reflection exception. Should only be called if
	 * no checked exception is expected to be thrown by the target method.
	 * <p>Throws the underlying RuntimeException or Error in case of an
	 * InvocationTargetException with such a root cause. Throws an
	 * IllegalStateException with an appropriate message else.
	 * @param ex the reflection exception to handle
	 */
	public static void handleReflectionException(Exception ex) {
		if (ex instanceof NoSuchMethodException) {
			throw new IllegalStateException("Method not found: " + ex.getMessage());
		}
		if (ex instanceof IllegalAccessException) {
			throw new IllegalStateException("Could not access method: " + ex.getMessage());
		}
		if (ex instanceof InvocationTargetException) {
			handleInvocationTargetException((InvocationTargetException) ex);
		}
		throw new IllegalStateException(
				"Unexpected reflection exception - " + ex.getClass().getName() + ": " + ex.getMessage());
	}

	/**
	 * Handle the given invocation target exception. Should only be called if
	 * no checked exception is expected to be thrown by the target method.
	 * <p>Throws the underlying RuntimeException or Error in case of such
	 * a root cause. Throws an IllegalStateException else.
	 * @param ex the invocation target exception to handle
	 */
	public static void handleInvocationTargetException(InvocationTargetException ex) {
		if (ex.getTargetException() instanceof RuntimeException) {
			throw (RuntimeException) ex.getTargetException();
		}
		if (ex.getTargetException() instanceof Error) {
			throw (Error) ex.getTargetException();
		}
		throw new IllegalStateException(
				"Unexpected exception thrown by method - " + ex.getTargetException().getClass().getName() +
				": " + ex.getTargetException().getMessage());
	}
}
