/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.hk2.utilities.reflection;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Arrays;

import org.glassfish.hk2.utilities.general.GeneralUtilities;

/**
 * An implementation of GenericArrayType for those times we need to create
 * this on the fly
 * 
 * @author jwells
 *
 */
public class GenericArrayTypeImpl implements GenericArrayType {
    private final Type genericComponentType;
    
    /**
     * Creates the GenericArrayType with the given array type
     * 
     * @param gct the non-null type for this GenericArrayType
     */
    public GenericArrayTypeImpl(Type gct) {
        genericComponentType = gct;
    }

    /* (non-Javadoc)
     * @see java.lang.reflect.GenericArrayType#getGenericComponentType()
     */
    @Override
    public Type getGenericComponentType() {
        return genericComponentType;
    }
    
    @Override
    public int hashCode() {
        return genericComponentType.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof GenericArrayType)) return false;
        
        GenericArrayType other = (GenericArrayType) o;
        
        return GeneralUtilities.safeEquals(genericComponentType, other.getGenericComponentType());
    }
    
    public String toString() {
        return "GenericArrayTypeImpl(" + genericComponentType + ")";
    }

}
