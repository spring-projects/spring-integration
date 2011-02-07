/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.build.bundlor

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.logging.LogLevel


/**
 * Contribute a 'bundlor' task capable of creating an OSGi manifest. Task is tied
 * to the lifecycle by having the 'jar' task depend on 'bundlor'.  Applies the 'java'
 * plugin to the project if it has not already been applied.
 *
 * @author Chris Beams
 * @author Luke Taylor
 * @see http://www.springsource.org/bundlor
 * @see http://static.springsource.org/s2-bundlor/1.0.x/user-guide/html/ch04s02.html
 */
public class BundlorPlugin implements Plugin<Project> {

    public void apply(Project project) {
        // bundlor plugin functionality only makes sense for java projects
        // if the java plugin is already applied, the following is a no-op
        project.getPlugins().apply(JavaPlugin.class)

        // configuration that will be used when creating the ant taskdef classpath
        project.configurations { bundlorconf }
        project.dependencies {
            bundlorconf 'com.springsource.bundlor:com.springsource.bundlor.ant:1.0.0.RELEASE',
                    'com.springsource.bundlor:com.springsource.bundlor:1.0.0.RELEASE',
                    'com.springsource.bundlor:com.springsource.bundlor.blint:1.0.0.RELEASE'
        }

        project.tasks.add("bundlor") {
            dependsOn project.compileJava
            description = 'Generates an OSGi-compatibile MANIFEST.MF file.'

            /* TODO
            // prescriptive defaults
            bundleName = project.description
            bundleVersion = project.version
            bundleVendor = 'SpringSource'
            //TODO bundleSymbolicName = project.basePackage
            bundleSymbolicName = 'replace-me-with-base-package'
            bundleManifestVersion = '2'
            */

            def template = new File(project.projectDir, 'template.mf')
            def bundlorDir = new File("${project.buildDir}/bundlor")
            def manifest = new File("${bundlorDir}/META-INF/MANIFEST.MF")

            // inform gradle what directory this task writes so that
            // it can be removed when issuing `gradle cleanBundlor`
            outputs.dir bundlorDir

            // incremental build configuration
            //   if the manifest output file already exists, the bundlor
            //   task will be skipped *unless* any of the following are true
            //   * template.mf has been changed
            //   * main classpath dependencies have been changed
            //   * main java sources for this project have been modified
            outputs.files manifest
            inputs.files template, project.sourceSets.main.runtimeClasspath

            // the bundlor manifest should be evaluated as part of the jar task's
            // incremental build
            project.jar {
                dependsOn 'bundlor'
                inputs.files manifest
            }

            project.jar.manifest.from manifest

            doFirst {
                project.ant.taskdef(
                    resource: 'com/springsource/bundlor/ant/antlib.xml',
                    classpath: project.configurations.bundlorconf.asPath)

                // the bundlor ant task writes directly to standard out
                // redirect it to INFO level logging, which gradle will
                // deal with gracefully
                logging.captureStandardOutput(LogLevel.INFO)

                // TODO tell the jar task to use bundlor manifest instead of the default
                // and customize it with all common headers
                //project.jar.manifest {
                    //from manifest
                    //attributes['Bundle-SymbolicName'] = bundleSymbolicName
                    //attributes['Bundle-Name'] = bundleName
                    //attributes['Bundle-Vendor'] = bundleVendor
                    //attributes['Bundle-Version'] = bundleVersion
                    //attributes['Bundle-ManifestVersion'] = bundleManifestVersion
                //}

                // the ant task will throw unless this dir exists
                if (!bundlorDir.isDirectory())
                    bundlorDir.mkdir()

                // execute the ant task, and write out the manifest file
                project.ant.bundlor(
                        inputPath: project.sourceSets.main.classesDir,
                        outputPath: bundlorDir,
                        manifestTemplatePath: template) {
                    property(name: 'version', value: project.version)
                }
            }
        }
    }
}
