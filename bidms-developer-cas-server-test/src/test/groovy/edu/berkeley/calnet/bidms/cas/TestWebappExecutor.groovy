/*
 * Copyright (c) 2022, Regents of the University of California and
 * contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.berkeley.calnet.bidms.cas

import groovy.util.logging.Slf4j

@Slf4j
class TestWebappExecutor {
    Process process
    TestWebappProcessorConsoleMonitorThread consoleMonitorThread

    int portNumber

    TestWebappExecutor(int portNumber) {
        this.portNumber = portNumber
    }

    protected String[] getEnvironment() {
        return [
                "JAVA_OPTS=" +
                        "-Dserver.port=$portNumber " +
                        "-Dserver.ssl.keyStore=${projectDir}/test-cas/testkeystore.jks " +
                        "-Dserver.ssl.keyStorePassword=changeit " +
                        "-Dserver.ssl.keyPassword=changeit " +
                        "-Djavax.net.ssl.trustStore=${projectDir}/test-cas/testtruststore.jks " +
                        "-Djavax.net.ssl.trustStorePassword=changeit"
        ]
    }

    protected File getProjectDir() {
        return new File("${System.getProperty('user.dir')}${File.separatorChar}..").absoluteFile
    }

    Process run() {
        File executableWarFile = new File(".." + File.separatorChar + ["test-webapp", "build", "libs", "test-webapp.war"].join(File.separator))
        if (!executableWarFile.exists()) {
            throw new IllegalStateException("${executableWarFile} does not exist")
        }

        this.process = Runtime.getRuntime().exec([executableWarFile] as String[], environment)
        log.info("TEST-WEBAPP PID ${process.pid()}")
        this.consoleMonitorThread = new TestWebappProcessorConsoleMonitorThread(process)
        consoleMonitorThread.start()

        waitForPortToBecomeAvailable()
    }

    private void waitForPortToBecomeAvailable() {
        boolean connected = false
        for (int i = 0; i < 10; i++) {
            if (!process.isAlive()) {
                sleep(1000)
                consoleMonitorThread.requestedStop = true
                throw new RuntimeException("test-webapp has exited prematurely with exit value ${process.exitValue()}")
            }
            try {
                def s = new Socket("localhost", portNumber)
                s.close()
            }
            catch (ConnectException e) {
                log.info "test-webapp port is not available yet"
                sleep(1000)
                continue
            }
            log.info "test-webapp port has become available"
            connected = true
            break
        }
        if (!connected) {
            shutdown()
            throw new RuntimeException("Test webapp port never became available")
        }
    }

    void shutdown() {
        boolean exited = ProcessDestructionUtil.destroyProcessTree(process, 10)
        consoleMonitorThread.requestedStop = true
        if (!exited || process.isAlive()) {
            throw new RuntimeException("Unable to shutdown test-app properly")
        } else {
            log.info("Exited")
        }
    }

    @Slf4j
    static class TestWebappProcessorConsoleMonitorThread extends Thread {
        final Process process
        volatile boolean requestedStop
        volatile boolean stopped

        TestWebappProcessorConsoleMonitorThread(Process process) {
            this.process = process
            daemon = true
            name = "testAppProcessConsoleMonitorThread"
        }

        @Override
        void run() {
            try (def reader = new BufferedReader(new InputStreamReader(process.inputStream))) {
                String line
                try {
                    while (!requestedStop && (line = reader.readLine()) != null) {
                        println "TEST-WEBAPP: $line"
                    }
                }
                catch (IOException e) {
                    log.warn("There was an error reading te test webapp process input stream", e)
                }
            }
            this.stopped = true
            log.info("Thread stopped")
        }
    }
}
