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
package com.sun.enterprise.module.common_impl;

import com.sun.enterprise.module.Module;
import com.sun.enterprise.module.ModuleChangeListener;
import com.sun.enterprise.module.ModuleDefinition;
import com.sun.enterprise.module.ModuleDependency;
import com.sun.enterprise.module.ModuleMetadata;
import com.sun.enterprise.module.ModuleState;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.ResolveError;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author rgrecour
 */
public class EmptyModule implements Module {

    protected final ModuleDefinition definition;
    protected final ModulesRegistry registry;
    private boolean sticky;
    private ModuleState state;
    private final ModuleMetadata metadata;

    public EmptyModule(ModuleDefinition definition, ModulesRegistry registry){
        this.definition = definition;
        this.registry = registry;
        this.state = ModuleState.NEW;
        this.metadata = new ModuleMetadata();
    }

    @Override
    public ModuleDefinition getModuleDefinition() {
        return definition;
    }

    @Override
    public String getName() {
        return definition.getName();
    }

    @Override
    public ModulesRegistry getRegistry() {
        return registry;
    }

    @Override
    public ModuleState getState() {
        return state;
    }

    @Override
    public void resolve() throws ResolveError {
        this.state = ModuleState.RESOLVED;
    }

    @Override
    public void start() throws ResolveError {
        this.state = ModuleState.READY;
    }

    @Override
    public boolean stop() {
        this.state = ModuleState.NEW;
        return true;
    }

    @Override
    public void detach() {
    }

    @Override
    public void refresh() {
    }

    @Override
    public ModuleMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void addListener(ModuleChangeListener listener) {
    }

    @Override
    public void removeListener(ModuleChangeListener listener) {
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }

    @Override
    public List<Module> getImports() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public void addImport(Module module) {
    }

    @Override
    public Module addImport(ModuleDependency dependency) {
        return this;
    }

    @Override
    public boolean isShared() {
        return false;
    }

    @Override
    public boolean isSticky() {
        return sticky;
    }

    @Override
    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }

    @Override
    public <T> Iterable<Class<? extends T>> getProvidersClass(Class<T> serviceClass) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Iterable<Class> getProvidersClass(String name) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public boolean hasProvider(Class serviceClass) {
        return false;
    }

    @Override
    public void dumpState(PrintStream writer) {
    }

    @Override
    public void uninstall() {
    }
}