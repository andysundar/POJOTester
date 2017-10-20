/*******************************************************************************
 * Copyright 2017 Anindya Bandopadhyay (anindyabandopadhyay@gmail.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.pojotester.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

import org.pojotester.log.PojoTesterLogger;
import org.pojotester.object.mock.MockDependencyObject;
import org.pojotester.object.mock.MockInterfaceObject;
import org.pojotester.reflection.annotation.ReflectionMethodLevel;

public abstract class ClassUtilities {
		

	public static ClassLoader getDefaultClassLoader(){
		ClassLoader classLoader = null;
		Thread currentThread = Thread.currentThread();
		if(currentThread != null) {
			// Thread context class loader 
			classLoader = currentThread.getContextClassLoader();
		} else {
			// Default class loader 
			classLoader = ClassUtilities.getDefaultClassLoader();
			if(classLoader == null) {
				// boot strap loader
				classLoader = ClassLoader.getSystemClassLoader();
			}
		}
		return classLoader;
	}
	
	public static Class<?> loadClass(final String className){
		ClassLoader classLoader = getDefaultClassLoader();
		Class<?> clazz = null;
		try {
			clazz = (classLoader != null) ?  classLoader.loadClass(className) : Class.forName(className);
		} catch (ClassNotFoundException e) {
			PojoTesterLogger.debugMessage("Not able to load " + className, e);
			e.printStackTrace();
		}
		return clazz;
	}
	
	public static Object createObject(final Class<?> clazz){
		Object object = null;
		try {
			object = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			PojoTesterLogger.debugMessage("Not able to initialize using default constructor of " + clazz.getName() + 
					".\n Now trying with other constructor (if any).", e);
			object =  createObjectUsingOtherConstructor(clazz);
		}
		return object;
	}
	
	public static Object createObjectUsingOtherConstructor(final Class<?> clazz){
		Object object = null;
		Constructor<?>[] constructors = clazz.getConstructors();
		for(Constructor<?> constructor : constructors){
			Parameter[] parameters = constructor.getParameters();
			Object[] args = new Object[parameters.length];
			int index = 0;
			for(Parameter parameter : parameters) {
				Class<?> parameterDataTypeClass = parameter.getType();
				Object obj = getValueFromMap(parameterDataTypeClass);
				if(obj != null) {
					args[index] = obj;
				} else if(parameterDataTypeClass.isEnum()) {
					args[index] = parameterDataTypeClass.getEnumConstants()[0];
				} else if(parameterDataTypeClass.isArray()){
					Class<?> typeOfArray = parameterDataTypeClass.getComponentType();
					args[index] = Array.newInstance(typeOfArray, 0);
				} else if(parameterDataTypeClass.isInterface()){
					MockInterfaceObject mockObject = new MockInterfaceObject(parameterDataTypeClass);
					args[index] = mockObject.proxy();
				} else {
					MockDependencyObject mockObject = new MockDependencyObject();
					args[index] = mockObject.getProxyObject(parameterDataTypeClass);
				} 
				index++;
			}
			try {
				object = constructor.newInstance(args);
				break;
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException ex) {
				PojoTesterLogger.debugMessage("Not able to initialize using other constructor of " + clazz.getName(), ex);
			}
		}
		return object;
	}
	
	public static Object createObjectUsingStaticMethod(final Class<?> clazz, Method method) {
		Object object = null;
		boolean isCreateMethod = ReflectionMethodLevel.isCreateMethod(method);
		int methodModifier = method.getModifiers();
		if (isCreateMethod && Modifier.isStatic(methodModifier) && Modifier.isPublic(methodModifier)) {
			try {
				object = method.invoke(null, null);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				PojoTesterLogger.debugMessage("Not able to invoke " + method.getName() + " in " + clazz.getName(), e);
			}
		}

		return object;
	}
	
	public static Object getValueFromMap(Class<?> clazz){
		return DefaultValueUtilities.getValueFromMap(clazz);
	}

}
