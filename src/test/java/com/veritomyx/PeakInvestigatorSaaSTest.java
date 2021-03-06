package com.veritomyx;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.mockito.Mockito.*;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import com.veritomyx.actions.BaseAction;
import com.veritomyx.actions.SftpAction;
import com.veritomyx.actions.SftpAction.SftpFingerprints;

public class PeakInvestigatorSaaSTest {

	private String server = null;
	private String username = null;
	private String password = null;
	private Integer port = null;
	private String fingerprint = null;

	private String filename = null;
	private boolean isInitialized = false;

	private static int TIMEOUT = 500; // milliseconds

	@Rule public ExpectedException thrown = ExpectedException.none();
	@Rule public TemporaryFolder tempFolder = new TemporaryFolder();

	@Before
	public void setUp() {
		server = System.getProperty("server");
		username = System.getProperty("user");
		password = System.getProperty("password");
		fingerprint = System.getProperty("fingerprint");

		try {
			port = Integer.parseInt(System.getProperty("port"));
		} catch (NumberFormatException e) {
			// don't do anything;
		}

		filename = System.getProperty("filename");

		isInitialized = server != null && username != null && password != null
				&& port != null && fingerprint != null && filename != null;
	}

	@Test
	public void testExecuteAction_BadServerTimeout() throws JSchException,
			IOException {

		assumeTrue("Credentials are not specified", isInitialized);

		thrown.expect(SocketTimeoutException.class);
		thrown.expectMessage("timed out");

		BaseAction action = mock(BaseAction.class);
		when(action.buildQuery()).thenReturn("Bad string");

		PeakInvestigatorSaaS service = new PeakInvestigatorSaaS(
				"unknown.veritomyx.com").withTimeout(TIMEOUT);
		service.executeAction(action);

		fail("Should not reach here.");
	}

	@Test
	public void testExecuteAction_BadServerName() throws JSchException,
			IOException {

		assumeTrue("Credentials are not specified", isInitialized);

		thrown.expect(UnknownHostException.class);
		thrown.expectMessage("unknown host");

		BaseAction action = mock(BaseAction.class);
		when(action.buildQuery()).thenReturn("Bad string");

		PeakInvestigatorSaaS service = new PeakInvestigatorSaaS(
				"http://unknown.veritomyx.com").withTimeout(TIMEOUT);
		service.executeAction(action);

		fail("Should not reach here.");
	}

	@Test
	public void testInitializeSftpSession_OK() throws JSchException {
		assumeTrue("Credentials are not specified", isInitialized);

		PeakInvestigatorSaaS service = new PeakInvestigatorSaaS(server);
		service.initializeSftpSession(server, username, password, port, fingerprint);

		assertTrue(service.isConnectedForSftp());
	}

	@Test
	public void testInitializeSftpSession_BadUsername() throws JSchException {
		assumeTrue("Credentials are not specified", isInitialized);

		thrown.expect(JSchException.class);
		thrown.expectMessage("Auth fail");

		PeakInvestigatorSaaS service = new PeakInvestigatorSaaS(server);
		service.initializeSftpSession(server, "joe", password, port, fingerprint);

		fail("Should not reach here.");
	}

	@Test
	public void testInitializeSftpSession_BadPassword() throws JSchException {
		assumeTrue("Credentials are not specified", isInitialized);

		thrown.expect(JSchException.class);
		thrown.expectMessage("Auth fail");

		PeakInvestigatorSaaS service = new PeakInvestigatorSaaS(server);
		service.initializeSftpSession(server, username, "code", port, fingerprint);

		fail("Should not reach here.");
	}

	@Test
	public void testInitializeSftpSession_BadPort() throws JSchException {
		assumeTrue("Credentials are not specified", isInitialized);

		thrown.expect(JSchException.class);
		thrown.expectMessage("connection is closed");

		PeakInvestigatorSaaS service = new PeakInvestigatorSaaS(server)
				.withTimeout(TIMEOUT);
		service.initializeSftpSession(server, username, password, 80, fingerprint);

		fail("Should not reach here.");
	}

	@Test
	public void testInitializeSftpSession_BadServer() throws JSchException {
		assumeTrue("Credentials are not specified", isInitialized);

		thrown.expect(JSchException.class);
		thrown.expectMessage("timeout");

		PeakInvestigatorSaaS service = new PeakInvestigatorSaaS("unknown.veritomyx.com")
				.withTimeout(TIMEOUT);
		service.initializeSftpSession("unknown.com", username, password, port, fingerprint);

		fail("Should not reach here.");
	}

	@Test
	public void testInitializeSftpSession_BadFingerprint() throws JSchException {
		assumeTrue("Credentials are not specified", isInitialized);

		thrown.expect(JSchException.class);
		thrown.expectMessage("Server identity is not correct");

		PeakInvestigatorSaaS service = new PeakInvestigatorSaaS(server)
				.withTimeout(TIMEOUT);
		service.initializeSftpSession(server, username, password, port,
				"5c:6f:c7:c7:79:c0:76:90:4d:3a:a1:7a:81:0e:0a:57");

		fail("Should not reach here.");
	}

	@Test
	public void testIntializeSftpSession_NotStarted() throws JSchException {
		assumeTrue("Credentials are not specified", isInitialized);

		PeakInvestigatorSaaS service = new PeakInvestigatorSaaS(server);
		assertFalse(service.isConnectedForSftp());
	}

	private SftpAction mockSftpAction() {
		SftpFingerprints fingerprints = mock(SftpFingerprints.class);
		when(fingerprints.getHash("RSA-MD5")).thenReturn(fingerprint);

		SftpAction action = mock(SftpAction.class);
		when(action.getHost()).thenReturn(server);
		when(action.getSftpUsername()).thenReturn(username);
		when(action.getSftpPassword()).thenReturn(password);
		when(action.getDirectory()).thenReturn("");
		when(action.getPort()).thenReturn(port);
		when(action.getFingerprints()).thenReturn(fingerprints);

		return action;
	}

	@Test
	public void testPutFile() throws JSchException, SftpException {
		assumeTrue("Credentials are not specified", isInitialized);

		PeakInvestigatorSaaS service = new PeakInvestigatorSaaS(server);
		SftpProgressMonitor sftpMonitor = new HeadlessProgressMonitor();
		File localFile = new File(filename);

		SftpAction action = mockSftpAction();
		service.putFile(action, filename, localFile.getName(),
				sftpMonitor);

		HeadlessProgressMonitor monitor = (HeadlessProgressMonitor) sftpMonitor;
		assertEquals(localFile.length(), monitor.transferredSize);
		assertEquals(localFile.length(), monitor.max);
		assertEquals(SftpProgressMonitor.PUT, monitor.direction);
		assertFalse(service.isConnectedForSftp());
	}

	@Test
	public void testGetFile() throws JSchException, SftpException, IOException {
		assumeTrue("Credentials are not specified", isInitialized);

		PeakInvestigatorSaaS service = new PeakInvestigatorSaaS(server);
		SftpProgressMonitor sftpMonitor = new HeadlessProgressMonitor();
		File localFile = new File(filename);
		File remoteFile = tempFolder.newFile(localFile.getName());

		SftpAction action = mockSftpAction();
		service.getFile(action, localFile.getName(),
				remoteFile.getAbsolutePath(), sftpMonitor);

		HeadlessProgressMonitor monitor = (HeadlessProgressMonitor) sftpMonitor;
		assertEquals(localFile.length(), monitor.transferredSize);
		assertEquals(localFile.length(), monitor.max);
		assertEquals(SftpProgressMonitor.GET, monitor.direction);
		assertFalse(service.isConnectedForSftp());
	}

	@SuppressWarnings("unused")
	private class HeadlessProgressMonitor implements SftpProgressMonitor {

		int direction;
		String src;
		String dst;
		long max;
		ArrayList<Long> counts = new ArrayList<>();
		long transferredSize = 0;

		@Override
		public void init(int direction, String src, String dst, long max) {
			this.direction = direction;
			this.src = src;
			this.dst = dst;
			this.max = max;
		}

		@Override
		public boolean count(long count) {
			transferredSize += count;
			counts.add(count);
			return true;
		}

		@Override
		public void end() {
			counts.trimToSize();
		}

	}
}
