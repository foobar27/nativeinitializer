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

import java.io.IOException;

public interface NativeLoader {

    void load() throws IOException;

    default NativeLoader fallbackTo(NativeLoader otherLoader) {
        return new NativeLoaderFactory.FallbackLoader(this, otherLoader);
    }

}