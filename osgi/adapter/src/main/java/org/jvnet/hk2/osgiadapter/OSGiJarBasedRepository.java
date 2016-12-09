/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved.
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
package org.jvnet.hk2.osgiadapter;

import com.sun.enterprise.module.ManifestConstants;
import com.sun.enterprise.module.ModuleDefinition;
import com.sun.enterprise.module.ModuleMetadata;
import com.sun.enterprise.module.common_impl.AbstractFactory;
import com.sun.enterprise.module.common_impl.AbstractRepositoryImpl;
import com.sun.enterprise.module.common_impl.Jar;
import com.sun.enterprise.module.common_impl.LogHelper;
import com.sun.enterprise.module.common_impl.ModuleId;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.utilities.DescriptorImpl;

/**
 *
 * @author Romain Grecourt
 */
public class OSGiJarBasedRepository extends AbstractRepositoryImpl {

    protected final URI repoUri;
    protected final URI jarUri;
    protected final JarFile repoJarFile;
    protected final String repoEntryName;
    private static final String JAR_SCHEME = "jar";
    private static final String JAR_SCHEME_FULL = JAR_SCHEME + ":";
    private static final String JAR_INNER_PATH_SEPARATOR = "!/";

    public static boolean isJarUri(URI uri){
       return JAR_SCHEME.equals(uri.getScheme());
    }

    public OSGiJarBasedRepository(URI repoUri) {
        super(repoUri.toString(), repoUri);
        if (!isJarUri(repoUri)) {
            throw new IllegalArgumentException(repoUri.toString() + " is not a jar URI");
        }
        try {
            String _repoUri = repoUri.toString();
            // URI with jar scheme pointing at the uber jar
            jarUri = URI.create(_repoUri.substring(0, _repoUri.lastIndexOf(JAR_INNER_PATH_SEPARATOR)));
            // URI with file scheme pointing at the uber jar
            URI jarFileURI = URI.create(jarUri.toString().substring(JAR_SCHEME_FULL.length()));
            // name of the jar entry of the repository within the uber jar
            repoEntryName = _repoUri.substring(jarUri.toString().length() + JAR_INNER_PATH_SEPARATOR.length());
            // the uber jar
            repoJarFile = new JarFile(new File(jarFileURI));
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
        this.repoUri = repoUri;
    }

    private static abstract class AbstractIterator<T> implements Iterator<T> {
        
        protected final Stack<T> items = new Stack<T>();
        protected abstract void processOne();

        @Override
        public boolean hasNext() {
            processOne();
            return !items.isEmpty();
        }

        @Override
        public T next() {
            processOne();
            if (items.isEmpty()) {
                return null;
            }
            return items.pop();
        }
    }

    private static class JarEntryIterator extends AbstractIterator<JarEntry>{

        private final Enumeration<JarEntry> entries;
        private final String prefix;

        public JarEntryIterator(JarFile repoJarFile, String prefix) {
            this.entries = repoJarFile.entries();
            this.prefix = prefix.replace("//", "/");
        }

        @Override
        protected void processOne() {
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith(prefix)) {
                    items.push(entry);
                    return;
                }
            }
        }
    }

    private static class JarInputStreamIterator extends AbstractIterator<Entry<JarEntry,InputStream>> {

        private final JarInputStream is;
        private final String prefix;

        public JarInputStreamIterator(JarInputStream is, String prefix) {
            this.is = is;
            this.prefix = prefix.replace("//", "/");
        }

        @Override
        protected void processOne() {
            JarEntry entry;
            try {
                while ((entry = is.getNextJarEntry()) != null) {
                    byte[] buffer = new byte[1024 * 4];
                    int n = 0;
                    long count = 0;
                    long size = entry.getSize();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    boolean found = entry.getName().startsWith(prefix);
                    while (-1 != (n = is.read(buffer)) && count < size) {
                        if (found) {
                            out.write(buffer, 0, n);
                        }
                        count += n;
                    }
                    if(found){
                        items.push(new SimpleEntry<JarEntry,InputStream>(
                                entry,
                                new ByteArrayInputStream(out.toByteArray())));
                        return;
                    }
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static URI makeEntryUri(URI jarUri, String entryName){
        StringBuilder sb = new StringBuilder();
        if(!isJarUri(jarUri)){
            sb.append(JAR_SCHEME).append(":");
        }
        sb.append(jarUri.toString()).append(JAR_INNER_PATH_SEPARATOR).append(entryName);
        return URI.create(sb.toString());
    }

    private URI makeEntryUri(String entryName){
        return makeEntryUri(this.jarUri,entryName);
    }

    private ModuleDefinition loadJar(URI jar) throws IOException {
        UberJarJar uberJar = new UberJarJar(jar, repoEntryName);
        if (uberJar.getManifest() != null) {
            Attributes attr = uberJar.getManifest().getMainAttributes();
            String bundleName = attr.getValue(ManifestConstants.BUNDLE_NAME);
            if (bundleName != null) {
                return new OSGiModuleDefinition(new UberJarJar(jar, repoEntryName), jar);
            }
        }
        return null;
    }

    @Override
    protected void loadModuleDefs(Map<ModuleId, ModuleDefinition> moduleDefs, List<URI> libraries) throws IOException {
        Iterator<JarEntry> it = new JarEntryIterator(repoJarFile, repoEntryName);
        while (it.hasNext()) {
            JarEntry entry = it.next();
            if (!entry.isDirectory() && entry.getName().endsWith(".jar")) {
                URI entryURI = makeEntryUri(entry.getName());
                ModuleDefinition moduleDef = loadJar(entryURI);
                if (moduleDef != null) {
                    moduleDefs.put(AbstractFactory.getInstance().createModuleId(moduleDef), moduleDef);
                } else {
                    libraries.add(entryURI);
                }
            }
        }
    }

    private static class UberJarJar extends Jar {

        private static final String HK2_DESCRIPTOR_LOCATION = "META-INF/hk2-locator";
        private static final String SERVICE_LOCATION = "META-INF/services";
        private final URI jarUri;
        private final String repoEntryName;
        private Manifest manifest = null;

        public UberJarJar(URI jarUri, String repoEntryName){
            this.jarUri = jarUri;
            this.repoEntryName = repoEntryName;
        }

        @Override
        public Manifest getManifest() throws IOException {
            if(manifest != null){
                return manifest;
            }
            JarInputStream is = new JarInputStream(jarUri.toURL().openStream());
            manifest = is.getManifest();
            return manifest;
        }

        @Override
        public void loadMetadata(ModuleMetadata result) {
            try {
                JarInputStream is = new JarInputStream(jarUri.toURL().openStream());
                parseServiceDescriptors(is, result);
                is = new JarInputStream(jarUri.toURL().openStream());
                parseDescriptors(is, result);
            } catch (IOException ex) {
                LogHelper.getDefaultLogger().log(Level.SEVERE,
                        "Error loading metadata for" + jarUri.toString(), ex);
            }
        }

        private void parseServiceDescriptors(JarInputStream jarInputStream, ModuleMetadata result) {
            Iterator<Entry<JarEntry, InputStream>> it = new JarInputStreamIterator(jarInputStream, SERVICE_LOCATION);
            while (it.hasNext()) {
                Entry<JarEntry, InputStream> entry = it.next();
                if (entry.getKey().isDirectory()) {
                    continue;
                }
                String serviceName = entry.getKey().getName().substring(SERVICE_LOCATION.length() + 1);
                final URI entryUri = makeEntryUri(jarUri, entry.getKey().getName());
                InputStream is = entry.getValue();
                try {
                    result.load(entryUri.toURL(), serviceName, is);
                } catch (IOException e) {
                    LogHelper.getDefaultLogger().log(Level.SEVERE,
                            "Error reading service provider in " + entryUri.toString(), e);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        }

        void parseDescriptors(JarInputStream is, ModuleMetadata result) {
            Iterator<Entry<JarEntry, InputStream>> it = new JarInputStreamIterator(is, HK2_DESCRIPTOR_LOCATION);
            while (it.hasNext()) {
                Entry<JarEntry, InputStream> entry = it.next();
                if(entry.getKey().isDirectory()){
                    continue;
                }
                String serviceLocatorName = entry.getKey().getName().substring(HK2_DESCRIPTOR_LOCATION.length() + 1);
                final URI entryUri = makeEntryUri(jarUri, entry.getKey().getName());
                List<Descriptor> descriptors = new ArrayList<Descriptor>();
                InputStream entryIs = entry.getValue();
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(entryIs));
                    try {
                        boolean readOne;
                        do {
                            DescriptorImpl descriptorImpl = new DescriptorImpl();
                            readOne = descriptorImpl.readObject(br);
                            if (readOne) {
                                descriptors.add(descriptorImpl);
                            }
                        } while (readOne);
                    } finally {
                        br.close();
                    }
                    result.addDescriptors(serviceLocatorName, descriptors);
                } catch (IOException e) {
                    LogHelper.getDefaultLogger().log(Level.SEVERE,
                            "Error reading descriptor in " + entryUri.toString(), e);
                } finally {
                    if (entryIs != null) {
                        try {
                            entryIs.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        }

        @Override
        public String getBaseName() {
            String _uri = jarUri.toString();
            return _uri.substring(_uri.lastIndexOf(repoEntryName));
        }
    }
}
