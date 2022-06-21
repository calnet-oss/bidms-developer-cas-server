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

import java.util.concurrent.TimeUnit

@Slf4j
class ProcessDestructionUtil {
    private static enum DestroyMethod {
        NORMAL, FORCIBLY
    }

    // returns true if process exited
    private static boolean destroySingleProcess(ProcessHandle proc, int timeoutSeconds, DestroyMethod destroyMethod) {
        if (!proc.isAlive()) {
            return true
        }
        def onExit = proc.onExit()
        if (destroyMethod == DestroyMethod.NORMAL) {
            proc.destroy()
        } else {
            proc.destroyForcibly()
        }
        onExit.get(timeoutSeconds, TimeUnit.SECONDS)
        return !proc.isAlive()
    }

    // returns true if process exited
    private static boolean destroySingleProcessNormally(ProcessHandle proc, int timeoutSeconds) {
        return destroySingleProcess(proc, timeoutSeconds, DestroyMethod.NORMAL)
    }

    // returns true if process exited
    private static boolean destroySingleProcessForcibly(ProcessHandle proc, int timeoutSeconds) {
        return destroySingleProcess(proc, timeoutSeconds, DestroyMethod.FORCIBLY)
    }

    // returns true if process exited
    private static boolean destroySingleProcess(ProcessHandle proc, int timeoutSeconds) {
        if (!destroySingleProcessNormally(proc, timeoutSeconds)) {
            log.warn "Unable to destroy PID ${proc.pid()} nicely.  Attempting to do it forcibly."
            return destroySingleProcessForcibly(proc, timeoutSeconds)
        } else {
            return true
        }
    }

    /**
     * Each PID in the tree may have up to two attempts: the first attempt
     * where there is an attempt to destroy the process normally and, if
     * that fails, an attempt to destroy the process forcibly.
     *
     * @param proc Root PID of process tree to destroy.
     * @param eachAttemptTimeoutSeconds Set the timeout period in seconds for each PID process attempt.
     * @return true if all processes exited
     */
    static boolean destroyProcessTree(ProcessHandle proc, int eachAttemptTimeoutSeconds) {
        boolean allDestroyed = true
        proc.descendants().each {
            if (!destroySingleProcess(it, eachAttemptTimeoutSeconds)) {
                log.warn "Unable to destroy descendant process with PID ${it.pid()}"
                allDestroyed = false
            }
        }
        if (!destroySingleProcess(proc, eachAttemptTimeoutSeconds)) {
            log.warn "Unable to destroy process PID ${proc.pid()}"
            allDestroyed = false
        }
        return allDestroyed
    }

    /**
     * {@link #destroyProcessTree(ProcessHandle, int)}
     */
    static boolean destroyProcessTree(Process proc, int eachAttemptTimeoutSeconds) {
        return destroyProcessTree(proc.toHandle(), eachAttemptTimeoutSeconds)
    }
}
