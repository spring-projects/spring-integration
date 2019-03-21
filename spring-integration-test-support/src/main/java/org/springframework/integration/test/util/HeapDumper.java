/*
 * Copyright 2015-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.test.util;

/**
 * Use to take a heap dump programmatically. Useful to examine the heap when debugging
 * sporadic test failures.
 * <p>Commented out because it uses access-restricted classes and some IDEs won't build
 * the project without relaxing the access restriction.
 * <p>Usage: {@code HeapDumper.dumpHeap("/tmp/foo.hprof");}
 * <p>If the file exists already, it will be replaced.
 * <p>Courtesy:
 * https://blogs.oracle.com/sundararajan/entry/programmatically_dumping_heap_from_java
 * <pre>
 * See https://docs.oracle.com/javase/8/docs/jre/api/management/extension/com/sun/management/HotSpotDiagnosticMXBean.html#dumpHeap-java.lang.String-boolean-
 * </pre>
 * @author Gary Russell
 * @since 4.2
 *
 */
public class HeapDumper {

//	// This is the name of the HotSpot Diagnostic MBean
//	private static final String HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";
//
//	// field to store the hotspot diagnostic MBean
//	private static volatile HotSpotDiagnosticMXBean hotspotMBean;
//
//	private HeapDumper() {
//		super();
//	}
//
//	public static void dumpHeap(String fileName) {
//		dumpHeap(fileName, true);
//	}
//
//	public static void dumpHeap(String fileName, boolean live) {
//		File file = new File(fileName);
//		if (file.exists()) {
//			file.delete();
//		}
//		// initialize hotspot diagnostic MBean
//		initHotspotMBean();
//		try {
//			hotspotMBean.dumpHeap(fileName, live);
//		}
//		catch (RuntimeException re) {
//			throw re;
//		}
//		catch (Exception exp) {
//			throw new RuntimeException(exp);
//		}
//	}
//
//	// initialize the hotspot diagnostic MBean field
//	private static void initHotspotMBean() {
//		if (hotspotMBean == null) {
//			synchronized (Object.class) {
//				if (hotspotMBean == null) {
//					hotspotMBean = getHotspotMBean();
//				}
//			}
//		}
//	}
//
//	// get the hotspot diagnostic MBean from the
//	// platform MBean server
//	private static HotSpotDiagnosticMXBean getHotspotMBean() {
//		try {
//			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
//			HotSpotDiagnosticMXBean bean = ManagementFactory.newPlatformMXBeanProxy(server, HOTSPOT_BEAN_NAME,
//					HotSpotDiagnosticMXBean.class);
//			return bean;
//		}
//		catch (RuntimeException re) {
//			throw re;
//		}
//		catch (Exception exp) {
//			throw new RuntimeException(exp);
//		}
//	}

}
