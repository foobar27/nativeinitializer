/*
 * Copyright (c) 2017 Sebastien Wagener.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sebastien Wagener - initial release
 */
package com.github.foobar27.nativeinitializer;

public interface NamingScheme {

    String getLibraryName();

    /**
     * Tries to determine the file name, depending on the current platform (os architecture and name).
     *
     * @return A relative file name
     */
    String determineName();

}
