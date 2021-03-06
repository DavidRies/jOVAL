// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.io;

import java.io.DataInput;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.cal10n.LocLogger;

import org.apache.jdbm.Serializer;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IReader;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.io.IUnixFileInfo;
import org.joval.intf.unix.io.IUnixFilesystem;
import org.joval.intf.unix.io.IUnixFilesystemDriver;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.IProperty;
import org.joval.intf.util.tree.INode;
import org.joval.io.fs.CacheFile;
import org.joval.io.fs.CacheFilesystem;
import org.joval.io.fs.DefaultFile;
import org.joval.io.fs.FileInfo;
import org.joval.io.BufferedReader;
import org.joval.io.PerishableReader;
import org.joval.io.StreamTool;
import org.joval.os.unix.io.driver.AIXDriver;
import org.joval.os.unix.io.driver.LinuxDriver;
import org.joval.os.unix.io.driver.MacOSXDriver;
import org.joval.os.unix.io.driver.SolarisDriver;
import org.joval.util.tree.Tree;
import org.joval.util.tree.Node;
import org.joval.util.JOVALMsg;
import org.joval.util.PropertyUtil;
import org.joval.util.SafeCLI;
import org.joval.util.StringTools;
import org.joval.util.tree.TreeHash;

/**
 * A local IFilesystem implementation for Unix.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnixFilesystem extends CacheFilesystem implements IUnixFilesystem {
    protected final static String LOCAL_INDEX		= "fs.index.gz";
    protected final static String INDEX_PROPS		= "fs.index.properties";
    protected final static String INDEX_PROP_COMMAND	= "command";
    protected final static String INDEX_PROP_USER	= "user";
    protected final static String INDEX_PROP_FLAVOR	= "flavor";
    protected final static String INDEX_PROP_MOUNTS	= "mounts";
    protected final static String INDEX_PROP_LEN	= "length";
    protected final static String DELIM_STR		= "/";
    protected final static char   DELIM_CH		= '/';

    protected long S, M, L, XL;
    protected boolean preloaded = false;
    protected IUnixSession us;

    private IUnixFilesystemDriver driver;
    private int entries, maxEntries;

    public UnixFilesystem(IBaseSession session, IEnvironment env) {
	super(session, env, null, DELIM_STR, "fs.db");
	us = (IUnixSession)session;
	S = us.getTimeout(IUnixSession.Timeout.S);
	M = us.getTimeout(IUnixSession.Timeout.M);
	L = us.getTimeout(IUnixSession.Timeout.L);
	XL= us.getTimeout(IUnixSession.Timeout.XL);
    }

    /**
     * Non-local subclasses should override this method.
     */
    protected String getPreloadPropertyKey() {
	return PROP_PRELOAD_LOCAL;
    }

    @Override
    protected int getDefaultFlags() {
	return super.getDefaultFlags();
    }

    @Override
    protected Serializer<IFile> getSerializer() {
	return new UnixCacheFileSerializer(this);
    }

    @Override
    protected IFile accessResource(String path, int flags) throws IllegalArgumentException, IOException {
	return new UnixFile(this, (CacheFile)super.accessResource(path, flags), path);
    }

    @Override
    protected boolean loadCache() {
	if (!props.getBooleanProperty(getPreloadPropertyKey())) {
	    return false;
	} else if (preloaded) {
	    return true;
	} else if (session.getType() != IBaseSession.Type.UNIX) {
	    return false;
	}

	//
	// Start from a clean cache.
	//
	reset();
	entries = 0;
	maxEntries = props.getIntProperty(PROP_PRELOAD_MAXENTRIES);
	try {
	    Collection<String> mounts = null;
	    String fsTypeFilter = props.getProperty(PROP_MOUNT_FSTYPE_FILTER);
	    if (fsTypeFilter == null) {
		mounts = getDriver().getMounts(null);
	    } else {
		mounts = getDriver().getMounts(Pattern.compile(fsTypeFilter));
	    }
	    for (String mount : mounts) {
		addRoot(mount);
	    }

	    String command = getDriver().getFindCommand();

	    if (VAL_FILE_METHOD.equals(props.getProperty(PROP_PRELOAD_METHOD))) {
		//
		// The file method stores the output of the find command in a file...
		//
		IReader reader = null;
		File wsdir = session.getWorkspace();
		IFile remoteCache = null, propsFile = null;
		boolean cleanRemoteCache = true;
		if(wsdir == null) {
		    //
		    // State cannot be saved locally, so create the file on the remote machine.
		    //
		    cleanRemoteCache = false;
		    remoteCache = getRemoteCache(command, mounts);
		    propsFile = getRemoteCacheProps();
		    reader = new BufferedReader(new GZIPInputStream(remoteCache.getInputStream()));
		} else {
		    //
		    // Read from the local state file, or create one while reading from the remote state file.
		    //
		    File local = new File(wsdir, LOCAL_INDEX);
		    IFile localCache = new DefaultFile(this, local, local.getAbsolutePath());
		    File localProps = new File(wsdir, INDEX_PROPS);
		    propsFile = new DefaultFile(this, localProps, localProps.getAbsolutePath());
		    Properties cacheProps = new Properties();
		    if (propsFile.exists()) {
			cacheProps.load(propsFile.getInputStream());
		    }
		    if (isValidCache(localCache, new PropertyUtil(cacheProps), command, mounts)) {
			reader = new BufferedReader(new GZIPInputStream(localCache.getInputStream()));
			cleanRemoteCache = false;
		    } else {
			remoteCache = getRemoteCache(command, mounts);
			OutputStream out = localCache.getOutputStream(false);
			StreamTool.copy(remoteCache.getInputStream(), out);
			out.close();
			reader = new BufferedReader(new GZIPInputStream(localCache.getInputStream()));
		    }
		}

		//
		// Store properties about the remote cache file.  If there is none, then we're using the verified local
		// cache file, so there's no new data to store.
		//
		if (remoteCache != null) {
		    Properties cacheProps = new Properties();
		    cacheProps.setProperty(INDEX_PROP_COMMAND, command);
		    cacheProps.setProperty(INDEX_PROP_USER, us.getEnvironment().getenv("LOGNAME"));
		    cacheProps.setProperty(INDEX_PROP_FLAVOR, us.getFlavor().value());
		    cacheProps.setProperty(INDEX_PROP_MOUNTS, alphabetize(mounts));
		    cacheProps.setProperty(INDEX_PROP_LEN, Long.toString(remoteCache.length()));
		    cacheProps.store(propsFile.getOutputStream(false), null);
		}

		addEntries(reader);
		if (cleanRemoteCache) {
		    remoteCache.delete();
		    if (remoteCache.exists()) {
			SafeCLI.exec("rm -f " + remoteCache.getPath(), session, IUnixSession.Timeout.S);
		    }
		}
	    } else {
		//
		// The stream method (default) reads directly from the stdout of the find command on the remote host.
		//
		for (String mount : mounts) {
		    IProcess p = null;
		    ErrorReader er = null;
		    IReader reader = null;
		    try {
			p = session.createProcess(command.replace("%MOUNT%", mount), null);
			p.start();
			reader = PerishableReader.newInstance(p.getInputStream(), S);
			er = new ErrorReader(PerishableReader.newInstance(p.getErrorStream(), XL));
			er.start();
			addEntries(reader);
		    } finally {
			//
			// Clean-up
			//
			if (p != null) {
			    p.waitFor(0);
			}
			if (er != null) {
			    er.join();
			}
		    }
		}
	    }
	    preloaded = true;
	    logger.info(JOVALMsg.STATUS_FS_PRELOAD_DONE, cacheSize());
	    return true;
	} catch (PreloadOverflowException e) {
	    logger.warn(JOVALMsg.ERROR_PRELOAD_OVERFLOW, maxEntries);
	    preloaded = true;
	    return true;
	} catch (Exception e) {
	    logger.warn(JOVALMsg.ERROR_PRELOAD);
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    return false;
	}
    }

    /**
     * This is really just an ersatz local test -- it merely checks whether the path is in the cache -- but it's
     * close enough for the purpose of avoiding performance-intensive probing of remote filesystems.
     */
    public boolean isLocalPath(String path) {
	loadCache();
	try {
	    peek(path);
	    return true;
	} catch (NoSuchElementException e) {
	    return false;
	}
    }

    public String getDelimiter() {
	return DELIM_STR;
    }

    // Implement IUnixFilesystem

    public IUnixFilesystemDriver getDriver() {
	if (driver == null) {
	    switch(us.getFlavor()) {
	      case AIX:
		driver = new AIXDriver(us);
		break;
	      case MACOSX:
		driver = new MacOSXDriver(us);
		break;
	      case LINUX:
		driver = new LinuxDriver(us);
		break;
	      case SOLARIS:
		driver = new SolarisDriver(us);
		break;
	      default:
		throw new RuntimeException(JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_UNIX_FLAVOR, us.getFlavor()));
	    }
	}
	return driver;
    }

    // Protected

    /**
     * Create a UnixFile from the output line of the stat command.
     */
    protected UnixFileInfo getUnixFileInfo(String path) throws Exception {
	String cmd = new StringBuffer(getDriver().getStatCommand()).append(" ").append(path).toString();
	List<String> lines = SafeCLI.multiLine(cmd, us, IUnixSession.Timeout.S);
	UnixFileInfo ufi = null;
	if (lines.size() > 0) {
	    ufi = (UnixFileInfo)getDriver().nextFileInfo(lines.iterator());
	    if (ufi == null) {
		throw new Exception(JOVALMsg.getMessage(JOVALMsg.ERROR_UNIXFILEINFO, path, lines.get(0)));
	    }
	} else {
	    logger.warn(JOVALMsg.ERROR_UNIXFILEINFO, path, "''");
	}
	return ufi;
    }

    // Private

    /**
     * Read entries from the cache file.
     */
    private void addEntries(IReader reader) throws PreloadOverflowException, IOException {
	UnixFileInfo ufi = null;
	ReaderIterator iter = new ReaderIterator(reader);
	while((ufi = (UnixFileInfo)getDriver().nextFileInfo(iter)) != null) {
	    if (ufi.path == null) {
		// bad entry -- skip it!
	    } else if (entries++ < maxEntries) {
		if (entries % 20000 == 0) {
		    logger.info(JOVALMsg.STATUS_FS_PRELOAD_FILE_PROGRESS, entries);
		}
		addToCache(ufi.path, new UnixFile(this, ufi, ufi.path));
	    } else {
		logger.warn(JOVALMsg.ERROR_PRELOAD_OVERFLOW, maxEntries);
		throw new PreloadOverflowException();
	    }
	}
    }

    /**
     * Check to see if the IFile represents a valid cache of the preload data.  This works on either a local or remote
     * copy of the cache.  The lastModified date is compared against the expiration, and if stale, the IFile is deleted.
     *
     * After the date is checked, the properties are used to validate the length of the file and the list of filesystem
     * mounts to be indexed.
     */
    private boolean isValidCache(IFile f, IProperty cacheProps, String command, Collection<String> mounts) throws IOException {
	try {
	    test(f.exists() && f.isFile(), "isFile");

	    //
	    // Check the expiration date
	    //
	    String s = PROP_PRELOAD_MAXAGE;
	    test(System.currentTimeMillis() < (f.lastModified() + props.getLongProperty(s)), s);

	    //
	    // Check the command
	    //
	    s = INDEX_PROP_COMMAND;
	    test(command.equals(cacheProps.getProperty(s)), s);

	    //
	    // Check the username
	    //
	    s = INDEX_PROP_USER;
	    test(us.getEnvironment().getenv("LOGNAME").equals(cacheProps.getProperty(s)), s);

	    //
	    // Check the Unix flavor
	    //
	    s = INDEX_PROP_FLAVOR;
	    test(us.getFlavor().value().equals(cacheProps.getProperty(s)), s);

	    //
	    // Check the length
	    //
	    s = INDEX_PROP_LEN;
	    test(f.length() == cacheProps.getLongProperty(s), s);

	    //
	    // Check the mounts
	    //
	    s = INDEX_PROP_MOUNTS;
	    test(alphabetize(mounts).equals(cacheProps.getProperty(s)), s);

	    logger.info(JOVALMsg.STATUS_FS_PRELOAD_CACHE_REUSE, f.getPath());
	    return true;
	} catch (AssertionError e) {
	    logger.warn(JOVALMsg.STATUS_FS_PRELOAD_CACHE_MISMATCH, f.getPath(), e.getMessage());
	    try {
		f.delete();
	    } catch (IOException ioe) {
	    }
	    return false;
	}
    }

    private static final String CACHE_DIR = "%HOME%";
    private static final String CACHE_TEMP = ".jOVAL.find.gz~";
    private static final String CACHE_FILE = ".jOVAL.find.gz";
    private static final String CACHE_PROPS = ".jOVAL.find.properties";

    private IFile getRemoteCacheProps() throws IOException {
	return getFile(env.expand(CACHE_DIR + DELIM_STR + CACHE_PROPS), IFile.READWRITE);
    }

    /**
     * Return a valid cache file on the remote machine, if available, or create a new one and return it.
     */
    private IFile getRemoteCache(String command, Collection<String> mounts) throws Exception {
	String tempPath = env.expand(CACHE_DIR + DELIM_STR + CACHE_TEMP);
	String destPath = env.expand(CACHE_DIR + DELIM_STR + CACHE_FILE);

	Properties cacheProps = new Properties();
	IFile propsFile = getRemoteCacheProps();
	if (propsFile.exists()) {
	    cacheProps.load(propsFile.getInputStream());
	}

	try {
	    IFile temp = getFile(destPath, IFile.READVOLATILE);
	    if (isValidCache(temp, new PropertyUtil(cacheProps), command, mounts)) {
		return temp;
	    }
	} catch (FileNotFoundException e) {
	}

	logger.info(JOVALMsg.STATUS_FS_PRELOAD_CACHE_TEMP, tempPath);
	boolean first = true;
	for (String mount : mounts) {
	    StringBuffer sb = new StringBuffer(command.replace("%MOUNT%", mount));
	    sb.append(" | gzip ");
	    if (first) {
		sb.append("> ");  // over-write
		first = false;
	    } else {
		sb.append(">> "); // append
	    }
	    sb.append(env.expand(tempPath)).toString();

	    IProcess p = session.createProcess(sb.toString(), null);
	    p.start();

	    //
	    // We have redirected stdout to the file, but we don't know which stream stderr can be read from, so
	    // we'll try reading from both!
	    //
	    ErrorReader er1 = new ErrorReader(PerishableReader.newInstance(p.getInputStream(), XL));
	    er1.start();
	    ErrorReader er2 = new ErrorReader(PerishableReader.newInstance(p.getErrorStream(), XL));
	    er2.start();

	    //
	    // Log a status update every 15 seconds while we wait, but wait for no more than an hour.
	    //
	    boolean done = false;
	    for (int i=0; !done && i < 240; i++) {
		for (int j=0; !done && j < 15; j++) {
		    if (p.isRunning()) {
			Thread.sleep(1000);
		    } else {
			done = true;
		    }
		}
		logger.info(JOVALMsg.STATUS_FS_PRELOAD_CACHE_PROGRESS, getFile(tempPath, IFile.READVOLATILE).length());
	    }
	    if (!done) {
		p.destroy();
	    }
	    er1.close();
	    er2.close();
	    er1.join();
	    er2.join();
	}

	//
	// Move the temp file; retry once if the attempt fails for some reason.
	//
	for (int i=0; i < 2; i++) {
	    if (SafeCLI.execData("mv " + tempPath + " " + destPath, null, session, S).getExitCode() == 0) {
		logger.info(JOVALMsg.STATUS_FS_PRELOAD_CACHE_CREATE, destPath);
		break;
	    }
	}
	return getFile(destPath);
    }

    /**
     * Pretty much exactly the same as an "assert" statement.
     */
    private void test(boolean val, String msg) throws AssertionError {
	if (!val) throw new AssertionError(msg);
    }

    private String alphabetize(Collection<String> sc) {
	String[] sa = sc.toArray(new String[sc.size()]);
	Arrays.sort(sa);
	StringBuffer sb = new StringBuffer();
	for (String s : sa) {
	    if (sb.length() > 0) {
		sb.append(":");
	    }
	    sb.append(s);
	}
	return sb.toString();
    }

    private class ReaderIterator implements Iterator<String> {
	IReader reader;
	String next = null;

	ReaderIterator(IReader reader) {
	    this.reader = reader;
	}

	// Implement Iterator<String>

	public boolean hasNext() {
	    if (next == null) {
		try {
		    next = next();
		    return true;
		} catch (NoSuchElementException e) {
		    return false;
		}
	    } else {
		return true;
	    }
	}

	public String next() throws NoSuchElementException {
	    if (next == null) {
		try {
		    if ((next = reader.readLine()) == null) {
			try {
			    reader.close();
			} catch (IOException e) {
			}
			throw new NoSuchElementException();
		    }
		} catch (IOException e) {
		    throw new NoSuchElementException(e.getMessage());
		}
	    }
	    String temp = next;
	    next = null;
	    return temp;
	}

	public void remove() {
	    throw new UnsupportedOperationException();
	}
    }

    private class ErrorReader implements Runnable {
	IReader err;
	Thread t;

	ErrorReader(IReader err) {
	    err.setLogger(logger);
	    this.err = err;
	}

	void start() {
	    t = new Thread(this);
	    t.start();
	}

	void join() throws InterruptedException {
	    t.join();
	}

	void close() {
	    if (t.isAlive()) {
		t.interrupt();
	    }
	}

	public void run() {
	    try {
		String line = null;
		while((line = err.readLine()) != null) {
		    logger.warn(JOVALMsg.ERROR_PRELOAD_LINE, line);
		}
	    } catch (InterruptedIOException e) {
		// ignore
	    } catch (IOException e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } finally {
		try {
		    err.close();
		} catch (IOException e) {
		}
	    }
	}
    }
}
