/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2016 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.sun.enterprise.module.single;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.enterprise.module.ModuleDefinition;
import com.sun.enterprise.module.ModuleDependency;
import com.sun.enterprise.module.ModuleMetadata;

/**
 * Creates a ModuleDefinition backed up by a a single classloader.
 *
 * The implementation does not cache any data - everything is recalculated for
 * each call. Callers are therefore encouraged to either supply their own
 * caching, or minimize the calls to methods of this class.
 *
 * @author Jerome Dochez
 */
public class ProxyModuleDefinition implements ModuleDefinition {

    private final ClassLoader classLoader;
    private final List<ManifestProxy.SeparatorMappings> mappings;

    public ProxyModuleDefinition(ClassLoader classLoader) throws IOException {
        this(classLoader, null);
    }

    public ProxyModuleDefinition(ClassLoader classLoader,
            List<ManifestProxy.SeparatorMappings> mappings) throws IOException {
        this.classLoader = classLoader;
        this.mappings = mappings;
    }

    @Override
    public String getName() {
        return "Static Module";
    }

    @Override
    public String[] getPublicInterfaces() {
        return new String[0];
    }

    @Override
    public ModuleDependency[] getDependencies() {
        return new ModuleDependency[0];
    }

    @Override
    public URI[] getLocations() {
        List<URI> uris = new ArrayList<URI>();
        if (classLoader instanceof URLClassLoader) {
            URLClassLoader urlCL = (URLClassLoader) classLoader;
            for (URL url : urlCL.getURLs()) {
                try {
                    uris.add(url.toURI());
                } catch (URISyntaxException e) {
                    Logger.getAnonymousLogger().log(Level.SEVERE, e.getMessage(), e);
                }
            }
        } else {
            String cp = System.getProperty("java.class.path");
            if (ok(cp)) {
                String[] paths = cp.split(System.getProperty("path.separator"));
                if (ok(paths)) {
                    for (String path : paths) {
                        uris.add(new File(path).toURI());
                    }
                }
            }
        }
        return uris.toArray(new URI[uris.size()]);
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getImportPolicyClassName() {
        return null;
    }

    @Override
    public String getLifecyclePolicyClassName() {
        return null;
    }

    @Override
    public Manifest getManifest() {
        return generate(new ModuleMetadata());
    }

    @Override
    public ModuleMetadata getMetadata() {
        ModuleMetadata metadata = new ModuleMetadata();
        generate(metadata);
        return metadata;
    }

    protected Manifest generate(ModuleMetadata metadata) {
        try {
            Manifest manifest = new ManifestProxy(classLoader, mappings);

            return manifest;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean ok(String s) {
        return s != null && s.length() > 0;
    }

    private static boolean ok(String[] ss) {
        return ss != null && ss.length > 0;
    }
}
