// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ssh.system;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.OutputStream;

import org.slf4j.cal10n.LocLogger;

import org.vngx.jsch.ChannelShell;
import org.vngx.jsch.ChannelType;
import org.vngx.jsch.Session;
import org.vngx.jsch.exception.JSchException;

import org.joval.io.PerishableReader;
import org.joval.util.JOVALMsg;

/**
 * A basic SSH shell-channel-based IProcess implementation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class BasicShellProcess extends SshProcess {
    static final int CR = '\r';
    static final byte[] MODE = {0x35, 0x00, 0x00, 0x00, 0x00, 0x00}; // echo off

    int exitValue = -1;

    BasicShellProcess(Session session, String command, String[] env, boolean debug, File wsdir, int pid, LocLogger logger)
		throws JSchException {

	super(command, env, debug, wsdir, pid, logger);
	channel = session.openChannel(ChannelType.SHELL);
    }

    // Implement IProcess

    @Override
    public void start() throws Exception {
	logger.debug(JOVALMsg.STATUS_SSH_PROCESS_START, Type.SHELL, command);
	((ChannelShell)channel).setPty(true);
	((ChannelShell)channel).setTerminalMode(MODE);
	channel.connect();
	getOutputStream();
	PerishableReader reader = PerishableReader.newInstance(getInputStream(), 10000L);
	determinePrompt(reader); // garbage - may include MOTD
	out.write("\r".getBytes());
	out.flush();
	String prompt = determinePrompt(reader);
	in = new MarkerTerminatedInputStream(reader, prompt);
	out.write(command.getBytes());
	out.write(CR);
	out.flush();
	running = true;
    }

    @Override
    public int exitValue() throws IllegalThreadStateException {
	if (isRunning()) {
	    throw new IllegalThreadStateException(command);
	}
	return exitValue;
    }

    // Internal

    /**
     * Get the exit code of the last command, given the prompt pattern.
     */
    int getExitValueInternal(InputStream in, String prompt) throws Exception {
	return -1;
    }

    /**
     * The prompt ends with, "$", ">" or "#", and possibly, a space after that.
     */
    String determinePrompt(InputStream in) throws IOException {
	int ch = -1;
	boolean trigger = false;
	StringBuffer sb = new StringBuffer();
	while ((ch = in.read()) != -1) {
	    switch(ch) {
	      case '>':
	      case '$':
	      case '#':
		sb.append((char)ch);
		if (in.available() == 0) {
		    return sb.toString();
		}
		trigger = true;
		break;

	      case ' ':
		sb.append((char)ch);
		if (trigger && in.available() == 0) {
		    return sb.toString();
		}
		trigger = false;
		break;

	      default:
		sb.append((char)ch);
		trigger = false;
		break;
	    }
	}
	return null;
    }

    class MarkerTerminatedInputStream extends PerishableReader {
	String marker;
	byte[] markerBytes;

	MarkerTerminatedInputStream(InputStream in, String marker) {
	    super(in, 10000L);
	    this.marker = marker;
	    markerBytes = marker.getBytes();
	}

	@Override
	public int read(byte[] buff, int offset, int len) throws IOException {
	    int bytesRead = 0;
	    if (buffer.hasNext()) {
		for (int i=offset; i < len && buffer.hasNext(); i++) {
		    buff[i] = (byte)(buffer.next() & 0xFF);
		    bytesRead++;
		}
		reset();
		return bytesRead;
	    } else if (!buffer.isEmpty()) {
		buffer.clear();
	    }

	    int avail = available();
	    if (avail == 0) {
		for (int i=offset; i < len; i++) {
		    int ch = read();
		    if (ch == -1) {
			break;
		    } else {
			buff[i] = (byte)ch;
			bytesRead++;
		    }
		}
		return bytesRead;
	    } else {
		byte[] temp = new byte[Math.min(avail, 512)];
		int tempLen = in.read(temp);
		for (int i=0; i < tempLen; i++) {
		    if (temp[i] == markerBytes[0] && markerTest(temp, i) == -1) {
			System.arraycopy(temp, 0, buff, offset, i);
			return i;
		    }
		}
		System.arraycopy(temp, 0, buff, offset, tempLen);
		return tempLen;
	    }
	}

	@Override
	public int read() throws IOException {
	    if (isEOF) {
		return -1;
	    }
	    if (buffer.hasNext()) {
		reset();
		return buffer.next();
	    } else if (!buffer.isEmpty()) {
		buffer.clear();
	    }
	    int ch = in.read();
	    reset();
	    if (ch == -1) {
		isEOF = true;
	    } else {
		if (buffer.hasCapacity()) {
		    buffer.add((byte)(ch & 0xFF));
		} else {
		    buffer.clear(); // buffer overflow
		}
		if (ch == (int)markerBytes[0]) {
		    return markerTest();
		}
	    }
	    return ch;
	}

	private int markerTest() throws IOException {
	    return markerTest(null, 0);
	}

	private int markerTest(byte[] buff, int offset) throws IOException {
	    boolean overflow = false;
	    for (int i=1; i < markerBytes.length; i++) {
		byte b;
		if (buff != null && (offset + i) < buff.length) {
		    b = buff[offset + i];
		} else {
		    if (!overflow) {
			setCheckpoint(markerBytes.length - i);
			overflow = true;
		    }
		    b = (byte)(in.read() & 0xFF);
		    buffer.add(b);
		    reset();
		}
		if (b != markerBytes[i]) {
		    if (overflow) {
			restoreCheckpoint();
		    }
		    return markerBytes[0];
		}
	    }
	    try {
		exitValue = getExitValueInternal(in, marker);
		close();
		running = false;
		channel.disconnect();
	    } catch (Exception e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    isEOF = true;
	    return -1;
	}
    }
}
