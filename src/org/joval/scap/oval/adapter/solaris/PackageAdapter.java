// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.solaris;

import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.solaris.PackageObject;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.core.EntityItemEVRStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.solaris.PackageItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;
import org.joval.util.Version;

/**
 * Evaluates Package OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PackageAdapter implements IAdapter {
    private IUnixSession session;
    private Hashtable<String, PackageItem> packageMap;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    packageMap = new Hashtable<String, PackageItem>();
	    classes.add(PackageObject.class);
	}
	return classes;
    }

    public Collection<PackageItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	PackageObject pObj = (PackageObject)obj;
	Collection<PackageItem> items = new Vector<PackageItem>();
	switch(pObj.getPkginst().getOperation()) {
	  case EQUALS:
	    try {
		items.add(getItem((String)pObj.getPkginst().getValue()));
	    } catch (NoSuchElementException e) {
		// package is not installed
	    } catch (Exception e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  case PATTERN_MATCH:
	    loadFullPackageMap();
	    try {
		Pattern p = Pattern.compile((String)pObj.getPkginst().getValue());
		for (String pkginst : packageMap.keySet()) {
		    if (p.matcher(pkginst).find()) {
			items.add(packageMap.get(pkginst));
		    }
		}
	    } catch (PatternSyntaxException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  case NOT_EQUAL: {
	    loadFullPackageMap();
	    String pkginst = (String)pObj.getPkginst().getValue();
	    for (String key : packageMap.keySet()) {
		if (!pkginst.equals(key)) {
		    items.add(packageMap.get(key));
		}
	    }
	    break;
	  }

	  default: {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, pObj.getPkginst().getOperation());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	  }
	}

	return items;
    }

    // Private

    private boolean loaded = false;
    private void loadFullPackageMap() {
	if (loaded) return;

	try {
	    List<String> packages = new Vector<String>();
	    session.getLogger().trace(JOVALMsg.STATUS_SOLPKG_LIST);
	    for (String line : SafeCLI.multiLine("pkginfo -x", session, IUnixSession.Timeout.L)) {
		if (line.length() == 0) {
		    break;
		}
		switch(line.charAt(0)) {
		  case ' ':
		  case '\t':
		    break;

		  default:
		    StringTokenizer tok = new StringTokenizer(line);
		    if (tok.countTokens() > 0) {
			packages.add(tok.nextToken());
		    }
		    break;
		}
	    }
	    for (String pkg : packages) {
		try {
		    PackageItem item = getItem(pkg);
		    packageMap.put((String)item.getPkginst().getValue(), item);
		} catch (Exception e) {
		    session.getLogger().warn(JOVALMsg.ERROR_SOLPKG, pkg);
		    session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    }
	    loaded = true;
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    private static final String PKGINST		= "PKGINST:";
    private static final String NAME		= "NAME:";
    private static final String CATEGORY	= "CATEGORY:";
    private static final String ARCH		= "ARCH:";
    private static final String VERSION		= "VERSION:";
    private static final String BASEDIR		= "BASEDIR:";
    private static final String VENDOR		= "VENDOR:";
    private static final String DESC		= "DESC:";
    private static final String PSTAMP		= "PSTAMP:";
    private static final String INSTDATE	= "INSTDATE:";
    private static final String HOTLINE		= "HOTLINE:";
    private static final String STATUS		= "STATUS:";
    private static final String FILES		= "FILES:";
    private static final String ERROR		= "ERROR:";

    private PackageItem getItem(String pkginst) throws Exception {
	PackageItem item = packageMap.get(pkginst);
	if (item != null) {
	    return item;
	}

	session.getLogger().debug(JOVALMsg.STATUS_SOLPKG_PKGINFO, pkginst);
	item = Factories.sc.solaris.createPackageItem();
	boolean isInstalled = false;
	for (String line : SafeCLI.multiLine("/usr/bin/pkginfo -l " + pkginst, session, IUnixSession.Timeout.S)) {
	    line = line.trim();
	    if (line.length() == 0) {
		break;
	    } else if (line.startsWith(PKGINST)) {
		isInstalled = true;
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setValue(line.substring(PKGINST.length()).trim());
		item.setPkginst(type);
	    } else if (line.startsWith(NAME)) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setValue(line.substring(NAME.length()).trim());
		item.setName(type);
	    } else if (line.startsWith(DESC)) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setValue(line.substring(DESC.length()).trim());
		item.setDescription(type);
	    } else if (line.startsWith(CATEGORY)) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setValue(line.substring(CATEGORY.length()).trim());
		item.setCategory(type);
	    } else if (line.startsWith(VENDOR)) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setValue(line.substring(VENDOR.length()).trim());
		item.setVendor(type);
	    } else if (line.startsWith(VERSION)) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setValue(line.substring(VERSION.length()).trim());
		item.setPackageVersion(type);
	    }
	}

	if (isInstalled) {
	    packageMap.put(pkginst, item);
	    return item;
	} else {
	    throw new NoSuchElementException(pkginst);
	}
    }
}
