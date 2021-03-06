// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.system;

import java.io.IOException;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;

/**
 * A representation of a session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ISession extends IBaseSession {
    public void setWorkingDir(String path);

    /**
     * Get the path to the "temp" directory.
     */
    public String getTempDir() throws IOException;

    /**
     * Configure the session to delete the specified file when it is disconnected.
     *
     * @throws IllegalStateException if the session is not connected.
     */
    public void deleteOnDisconnect(IFile file) throws IllegalStateException;

    public IFilesystem getFilesystem();

    public IEnvironment getEnvironment();
}
